package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.support.TestPostgres;
import com.equestrian.equis.support.AdminOversightFixture;
import com.equestrian.equis.support.AdminOversightFixture.Coach;
import com.equestrian.equis.support.MutableClock;
import com.equestrian.equis.support.TrainerAccountsFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

import static com.equestrian.equis.support.AdminOversightFixture.LEVEL_TWO;
import static com.equestrian.equis.support.AdminOversightFixture.SAT_17;
import static com.equestrian.equis.support.AdminOversightFixture.TEN;
import static com.equestrian.equis.support.AdminOversightFixture.read;
import static com.equestrian.equis.support.AdminOversightFixture.rowOf;
import static com.equestrian.equis.support.TrainerAccountsFixture.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow W-C, reminder journeys N1–N4: W2's Remind trainer and the notice it leaves for the assessor. There is no
// trainer-side notice endpoint yet, so the notice table is read directly. The single rules are in
// BookingReminderApiTest.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Import(AdminOversightFixture.Config.class)
class ReminderJourneyTest {
    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @DynamicPropertySource
    static void admin(DynamicPropertyRegistry registry) {
        TrainerAccountsFixture.adminAccount(registry);
    }

    @TestBean
    Clock clock;

    static Clock clock() {
        return new MutableClock(START);
    }

    @Autowired
    AdminOversightFixture fx;

    Coach ahmed;
    Coach mona;
    Coach karim;
    UUID sat17;

    @BeforeEach
    void setUp() throws Exception {
        fx.reset();
        ahmed = fx.coach("Ahmed Said");
        mona = fx.coach("Mona Kamal");
        karim = fx.coach("Karim Nabil");
        fx.schedule();
        sat17 = fx.slotOn(SAT_17, TEN);
    }

    // N1
    @Test
    void remindingTwiceLeavesOneNoticeForTheAssessor() throws Exception {
        UUID sara = waitingBooking(ahmed, "Sara Ali");

        fx.remind(sara).andExpect(status().isOk());
        fx.remind(sara).andExpect(status().isOk());

        assertThat(fx.reminders(ahmed, sara)).isEqualTo(1);
        assertThat(fx.noticesFor(ahmed)).isEqualTo(1);
        fx.w2().andExpect(jsonPath(rowOf(sara) + ".remindedAt").isNotEmpty());
    }

    // N2
    @Test
    void onceTheNoticeIsReadANewReminderLeavesAnother() throws Exception {
        UUID sara = waitingBooking(ahmed, "Sara Ali");
        Instant first = Instant.parse(read(fx.remind(sara).andExpect(status().isOk()), "$.remindedAt"));

        fx.readNotices(ahmed);
        fx.accounts().advance(Duration.ofDays(1));
        Instant second = Instant.parse(read(fx.remind(sara).andExpect(status().isOk()), "$.remindedAt"));

        assertThat(fx.reminders(ahmed, sara)).isEqualTo(2);
        assertThat(second).isAfter(first);
        fx.w2().andExpect(jsonPath(rowOf(sara) + ".remindedAt").value(second.toString()));
    }

    // N3
    @Test
    void aRecordedResultCannotBeReminded() throws Exception {
        UUID sara = waitingBooking(ahmed, "Sara Ali");
        fx.recordResult(ahmed, sara, true, LEVEL_TWO).andExpect(status().isOk());

        fx.remind(sara)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("result_not_overdue"));
        assertThat(fx.noticesFor(ahmed)).isZero();
    }

    // N4. The coach leaves: the reminder goes to the new assessor only.
    @Test
    void aDisabledAssessorsResultIsHandedToAnotherTrainerAndRemindedThere() throws Exception {
        UUID omar = waitingBooking(karim, "Omar Hassan");
        fx.disable(karim).andExpect(status().isOk());

        fx.remind(omar)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("trainer_not_active"));
        fx.patchBooking(omar, "{ \"assessorId\": \"%s\" }".formatted(mona.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("waiting"))
                .andExpect(jsonPath("$.assessor.id").value(mona.id().toString()))
                .andExpect(jsonPath("$.actions", hasItem("remind")));
        fx.remind(omar).andExpect(status().isOk());

        assertThat(fx.reminders(mona, omar)).isEqualTo(1);
        assertThat(fx.noticesFor(karim)).isZero();
    }

    // Booked by this trainer for Sat 17 Oct 10:00; the clock then moves to 10:05, so the result is due.
    private UUID waitingBooking(Coach assessor, String name) throws Exception {
        UUID booking = fx.bookMember(assessor, name, fx.nextPhone(), sat17);
        fx.at(SAT_17, LocalTime.of(10, 5));
        return booking;
    }
}
