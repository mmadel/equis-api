package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.support.AdminOversightFixture;
import com.equestrian.equis.support.AdminOversightFixture.Coach;
import com.equestrian.equis.support.MutableClock;
import com.equestrian.equis.support.TrainerAccountsFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static com.equestrian.equis.support.AdminOversightFixture.LEVEL_TWO;
import static com.equestrian.equis.support.AdminOversightFixture.SAT_17;
import static com.equestrian.equis.support.AdminOversightFixture.TEN;
import static com.equestrian.equis.support.TrainerAccountsFixture.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A1–A2: the five E7 admin endpoints turn away callers without a token and trainers, and change nothing.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Testcontainers
@Import(AdminOversightFixture.Config.class)
class AdminOversightAccessTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

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
    UUID waiting;
    UUID rider;

    // A waiting booking (something to reschedule and remind) and a placed rider (something to move).
    @BeforeEach
    void setUp() throws Exception {
        fx.reset();
        ahmed = fx.coach("Ahmed Said");
        mona = fx.coach("Mona Kamal");
        fx.schedule();
        UUID sat17 = fx.slotOn(SAT_17, TEN);
        waiting = fx.bookMember(ahmed, "Sara Ali", fx.nextPhone(), sat17);
        UUID omar = fx.bookMember(ahmed, "Omar Hassan", fx.nextPhone(), sat17);
        fx.at(SAT_17, LocalTime.of(10, 30));
        rider = UUID.fromString(AdminOversightFixture.read(
                fx.recordResult(ahmed, omar, true, LEVEL_TWO).andExpect(status().isOk()), "$.riderId"));
    }

    // A1
    @Test
    void withoutATokenEveryEndpointIsUnauthorized() throws Exception {
        for (MockHttpServletRequestBuilder request : endpoints()) {
            fx.accounts().perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("unauthorized"));
        }
        assertNothingChanged();
    }

    // A2
    @Test
    void aTrainerIsForbiddenEverywhere() throws Exception {
        for (MockHttpServletRequestBuilder request : endpoints()) {
            fx.asCoach(ahmed, request)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("forbidden"));
        }
        assertNothingChanged();
    }

    // GET and PATCH bookings, remind, GET riders, PATCH riders/trainer.
    private List<MockHttpServletRequestBuilder> endpoints() {
        return List.of(
                get("/admin/bookings"),
                patch("/admin/bookings/" + waiting).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"assessorId\": \"%s\" }".formatted(mona.id())),
                post("/admin/bookings/" + waiting + "/remind"),
                get("/admin/riders"),
                patch("/admin/riders/trainer").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"riderIds\": [\"%s\"], \"trainerId\": \"%s\" }".formatted(rider, mona.id())));
    }

    private void assertNothingChanged() {
        assertThat(fx.assessorOf(waiting)).isEqualTo(ahmed.id());
        assertThat(fx.bookingStatus(waiting)).isEqualTo("upcoming");
        assertThat(fx.trainerOf(rider)).isEqualTo(ahmed.id());
        assertThat(fx.noticesFor(ahmed)).isZero();
        assertThat(fx.noticesFor(mona)).isZero();
    }
}
