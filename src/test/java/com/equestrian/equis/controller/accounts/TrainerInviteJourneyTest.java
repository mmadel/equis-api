package com.equestrian.equis.controller.accounts;

import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.dto.accounts.TrainerRow;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.equestrian.equis.support.TrainerAccountsFixture.START;
import static com.equestrian.equis.support.TrainerAccountsFixture.TRAINERS;
import static com.equestrian.equis.support.TrainerAccountsFixture.rowPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow W-E, invite journeys I1–I6: the admin invites on W4, the email's link sets the password, the app signs in.
// Single rules and codes are in AdminTrainerApiTest and AdminTrainerActionsApiTest.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Testcontainers
@Import(TrainerAccountsFixture.Config.class)
class TrainerInviteJourneyTest {
    private static final String PASSWORD = "NewPass@2026";

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

    @BeforeEach
    void setUp() {
        fx.reset();
    }

    // I1
    @Test
    void anInvitedTrainerSetsAPasswordFromTheEmailAndSignsInOnTheApp() throws Exception {
        TrainerRow mona = fx.invite("Mona Fathy", "01012345678", "mona@example.com");

        assertThat(fx.recorder().all()).singleElement().satisfies(email -> {
            assertThat(email.to()).isEqualTo("mona@example.com");
            assertThat(email.subject()).isEqualTo("You're invited to Riding Schedule");
        });
        fx.acceptInvite(fx.tokenFromLastEmail(), PASSWORD).andExpect(status().isNoContent());
        fx.login("01012345678", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TRAINER"));
        AuthTokens tokens = fx.loginTrainer("01012345678", PASSWORD);
        fx.myLevels(tokens).andExpect(status().isOk());

        fx.list()
                .andExpect(jsonPath(rowPath(mona.id()) + ".status", contains("active")))
                .andExpect(jsonPath(rowPath(mona.id()) + ".pendingLink", contains(nullValue())))
                .andExpect(jsonPath(rowPath(mona.id()) + ".lastActiveAt", contains(START.toString())));
    }

    // I2
    @Test
    void anInviteLinkStopsWorkingAfter72Hours() throws Exception {
        TrainerRow mona = fx.invite("Mona Fathy", "01012345678", "mona@example.com");
        String token = fx.tokenFromLastEmail();
        fx.advance(Duration.ofHours(73));

        fx.acceptInvite(token, PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_link"));

        fx.list()
                .andExpect(jsonPath("$.totals.invited").value(1))
                .andExpect(jsonPath(rowPath(mona.id()) + ".status", contains("invited")))
                .andExpect(jsonPath(rowPath(mona.id()) + ".pendingLink.expired", contains(true)));
    }

    // I3
    @Test
    void resendKillsTheFirstEmailsLink() throws Exception {
        TrainerRow mona = fx.invite("Mona Fathy", "01012345678", "mona@example.com");
        String first = fx.tokenFromLastEmail();
        fx.admin(post(TRAINERS + "/" + mona.id() + "/resend-invite"))
                .andExpect(status().isOk());
        String second = fx.tokenFromLastEmail();

        assertThat(fx.recorder().all()).hasSize(2);
        fx.acceptInvite(first, PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_link"));
        fx.acceptInvite(second, PASSWORD).andExpect(status().isNoContent());
    }

    // I4
    @Test
    void aCancelledInviteLinkIsDeadAndThePhoneAndEmailCanBeInvitedAgain() throws Exception {
        TrainerRow mona = fx.invite("Mona Fathy", "01012345678", "mona@example.com");
        String token = fx.tokenFromLastEmail();

        fx.admin(delete(TRAINERS + "/" + mona.id() + "/invite"))
                .andExpect(status().isNoContent());
        fx.acceptInvite(token, PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_link"));

        fx.inviteRequest("Mona Fathy", "01012345678", "mona@example.com").andExpect(status().isCreated());
        assertThat(fx.recorder().all()).hasSize(2);
        assertThat(fx.tokenFromLastEmail()).isNotEqualTo(token);
    }

    // I5
    @Test
    void aTakenPhoneIsRefusedAndNoEmailIsSent() throws Exception {
        fx.invite("Mona Fathy", "01012345678", "mona@example.com");
        fx.recorder().clear();

        fx.inviteRequest("Mona Other", "01012345678", "mona.other@example.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("phone_taken"));
        assertThat(fx.recorder().all()).isEmpty();
    }

    // I6
    @Test
    void twoInvitesWithTheSamePhoneAtTheSameMomentMakeOneTrainer() throws Exception {
        fx.adminToken(); // signed in before the race, so both requests start together

        List<Integer> statuses = race(
                () -> fx.inviteRequest("Mona Fathy", "01012345678", "mona@example.com"),
                () -> fx.inviteRequest("Mona F.", "01012345678", "mona.f@example.com"));

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(fx.count("trainer")).isEqualTo(1);
        assertThat(fx.recorder().all()).hasSize(1);
    }

    // Starts both calls at once and gives each status code; the refused one must say phone_taken.
    private static List<Integer> race(Request first, Request second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> a = pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return statusOf(first);
            });
            Future<Integer> b = pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return statusOf(second);
            });
            start.countDown();
            return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    private static int statusOf(Request request) throws Exception {
        var response = request.send().andReturn().getResponse();
        if (response.getStatus() == 409) {
            assertThat(response.getContentAsString()).contains("\"phone_taken\"");
        }
        return response.getStatus();
    }

    private interface Request {
        ResultActions send() throws Exception;
    }
}
