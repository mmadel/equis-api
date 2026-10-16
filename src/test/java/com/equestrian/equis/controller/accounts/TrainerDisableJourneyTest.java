package com.equestrian.equis.controller.accounts;

import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.dto.accounts.TrainerRow;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.support.MutableClock;
import com.equestrian.equis.support.TrainerAccountsFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.equestrian.equis.support.TrainerAccountsFixture.START;
import static com.equestrian.equis.support.TrainerAccountsFixture.TRAINERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow W-E, disable and enable journeys D1–D7: what a disable does to a trainer who is signed in, to their links
// and to their riders, and what Enable gives back. The single rules are in TrainerAccessServiceTest.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Testcontainers
@Import(TrainerAccountsFixture.Config.class)
class TrainerDisableJourneyTest {
    private static final String PHONE = "01022223333";
    private static final String PASSWORD = "Ahmed@2026";
    private static final int LEVEL = 3;

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
    TrainerAccountsFixture fx;
    @Autowired
    RiderRepository riderRepository;

    // Coach Ahmed, signed in on two phones.
    TrainerRow ahmed;
    AuthTokens phone1;
    AuthTokens phone2;

    @BeforeEach
    void setUp() throws Exception {
        fx.reset();
        ahmed = fx.activeTrainer("Ahmed Said", PHONE, "ahmed@example.com", PASSWORD);
        phone1 = fx.loginTrainer(PHONE, PASSWORD);
        phone2 = fx.loginTrainer(PHONE, PASSWORD);
    }

    // D1
    @Test
    void disablingATrainerWithRidersAndABookingAsksForAConfirm() throws Exception {
        fx.addRiders(ahmed.id(), LEVEL, 3);
        fx.addUpcomingBooking(ahmed.id());

        disable(false)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("confirm_required"))
                .andExpect(jsonPath("$.riderCount").value(3))
                .andExpect(jsonPath("$.upcomingAssessmentCount").value(1));
        fx.myLevels(phone1).andExpect(status().isOk());
    }

    // D2
    @Test
    void aConfirmedDisableStopsTheSignedInTrainerAtOnce() throws Exception {
        fx.addRiders(ahmed.id(), LEVEL, 3);
        fx.addUpcomingBooking(ahmed.id());

        disable(true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("disabled"));

        fx.myLevels(phone1)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        fx.refresh(phone2)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_refresh_token"));
        fx.login(PHONE, PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("account_disabled"));
    }

    // D3
    @Test
    void aDisabledTrainersRidersNeedReassignmentAndKeepTheirSeats() throws Exception {
        List<UUID> riders = fx.addRiders(ahmed.id(), LEVEL, 3);
        UUID session = fx.addUpcomingSession(LEVEL);
        fx.assign(session, riders, ahmed.id());
        long disabledBefore = fx.total("disabled");
        long coveredBefore = fx.total("ridersCovered");

        disable(true).andExpect(status().isOk());

        assertThat(fx.total("disabled")).isEqualTo(disabledBefore + 1);
        assertThat(fx.total("ridersCovered")).isEqualTo(coveredBefore - 3);
        assertThat(riderRepository.findNeedingReassignment()).extracting(Rider::getId)
                .containsExactlyInAnyOrderElementsOf(riders);
        assertThat(seatsIn(session)).containsExactlyInAnyOrderElementsOf(riders);
    }

    // D4
    @Test
    void anOpenResetLinkDiesWithTheDisableAndEnableDoesNotBringItBack() throws Exception {
        fx.admin(post(TRAINERS + "/" + ahmed.id() + "/reset-password")).andExpect(status().isOk());
        String token = fx.tokenFromLastEmail();

        disable(false).andExpect(status().isOk());
        fx.acceptInvite(token, "NewPass@2026")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_link"));

        enable().andExpect(status().isOk());
        fx.acceptInvite(token, "NewPass@2026")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_link"));
    }

    // D5
    @Test
    void enableLetsTheTrainerSignInAgainButOldSessionsStayRevoked() throws Exception {
        fx.addRiders(ahmed.id(), LEVEL, 3);
        disable(true).andExpect(status().isOk());

        enable()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));

        fx.login(PHONE, PASSWORD).andExpect(status().isOk());
        fx.refresh(phone1)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_refresh_token"));
        assertThat(riderRepository.findNeedingReassignment()).isEmpty();
    }

    // D6. The clock moves between the calls, so a second write would show in updated_at and revoked_at.
    @Test
    void aRepeatedDisableOrEnableChangesNothing() throws Exception {
        disable(false).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("disabled"));
        Map<String, Object> afterFirst = state();
        fx.advance(Duration.ofMinutes(1));
        disable(false).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("disabled"));
        assertThat(state()).isEqualTo(afterFirst);

        enable().andExpect(status().isOk()).andExpect(jsonPath("$.status").value("active"));
        afterFirst = state();
        fx.advance(Duration.ofMinutes(1));
        enable().andExpect(status().isOk()).andExpect(jsonPath("$.status").value("active"));
        assertThat(state()).isEqualTo(afterFirst);
    }

    // D7
    @Test
    void actionsThatDoNotFitTheStatusAreRefused() throws Exception {
        TrainerRow mona = fx.invite("Mona Fathy", "01012345678", "mona@example.com");
        TrainerRow hany = fx.activeTrainer("Hany Adel", "01033334444", "hany@example.com", PASSWORD);
        fx.admin(post(TRAINERS + "/" + hany.id() + "/disable")).andExpect(status().isOk());

        refusedForStatus(fx.admin(post(TRAINERS + "/" + mona.id() + "/disable")));
        refusedForStatus(fx.admin(delete(TRAINERS + "/" + ahmed.id() + "/invite")));
        refusedForStatus(fx.admin(post(TRAINERS + "/" + hany.id() + "/reset-password")));

        assertThat(fx.statusOf(mona.id())).isEqualTo(TrainerStatus.INVITED);
        assertThat(fx.statusOf(ahmed.id())).isEqualTo(TrainerStatus.ACTIVE);
        assertThat(fx.statusOf(hany.id())).isEqualTo(TrainerStatus.DISABLED);
    }

    private ResultActions disable(boolean confirm) throws Exception {
        return fx.admin(post(TRAINERS + "/" + ahmed.id() + "/disable").param("confirm", String.valueOf(confirm)));
    }

    private ResultActions enable() throws Exception {
        return fx.admin(post(TRAINERS + "/" + ahmed.id() + "/enable"));
    }

    private static void refusedForStatus(ResultActions result) throws Exception {
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_trainer_status"));
    }

    private List<UUID> seatsIn(UUID session) {
        return fx.jdbc().queryForList("select rider_id from assignment where session_id = ?", UUID.class, session);
    }

    // Ahmed's row and refresh tokens as stored.
    private Map<String, Object> state() {
        return fx.jdbc().queryForMap("""
                select t.status, t.updated_at,
                       (select string_agg(coalesce(r.revoked_at::text, '-'), ',' order by r.created_at, r.id)
                        from refresh_token r where r.subject_id = t.id) as tokens
                from trainer t where t.id = ?""", ahmed.id());
    }
}
