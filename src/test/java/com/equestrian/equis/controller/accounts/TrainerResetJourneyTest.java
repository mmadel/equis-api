package com.equestrian.equis.controller.accounts;

import com.equestrian.equis.support.TestPostgres;
import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.dto.accounts.TrainerRow;
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
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Duration;

import static com.equestrian.equis.support.TrainerAccountsFixture.START;
import static com.equestrian.equis.support.TrainerAccountsFixture.TRAINERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow W-E, reset journeys R1–R3: the admin sends a set-password link to an active trainer who is signed in.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Import(TrainerAccountsFixture.Config.class)
class TrainerResetJourneyTest {
    private static final String PHONE = "01011112222";
    private static final String OLD_PASSWORD = "OldPass@2026";
    private static final String NEW_PASSWORD = "NewPass@2026";

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
    TrainerAccountsFixture fx;

    TrainerRow ahmed;
    AuthTokens phone1;
    AuthTokens phone2;

    @BeforeEach
    void setUp() throws Exception {
        fx.reset();
        ahmed = fx.activeTrainer("Ahmed Said", PHONE, "ahmed@example.com", OLD_PASSWORD);
        phone1 = fx.loginTrainer(PHONE, OLD_PASSWORD);
        phone2 = fx.loginTrainer(PHONE, OLD_PASSWORD);
    }

    // R1
    @Test
    void resetSendsOneEmailAndTheOldPasswordStillWorks() throws Exception {
        reset()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.pendingLink.purpose").value("reset"));

        assertThat(fx.recorder().all()).singleElement().satisfies(email -> {
            assertThat(email.to()).isEqualTo("ahmed@example.com");
            assertThat(email.subject()).isEqualTo("Set a new password for Riding Schedule");
        });
        fx.login(PHONE, OLD_PASSWORD).andExpect(status().isOk());
    }

    // R2
    @Test
    void usingTheResetLinkSignsOutEveryDeviceAndOnlyTheNewPasswordWorks() throws Exception {
        reset().andExpect(status().isOk());

        fx.acceptInvite(fx.tokenFromLastEmail(), NEW_PASSWORD).andExpect(status().isNoContent());

        for (AuthTokens device : new AuthTokens[]{phone1, phone2}) {
            fx.refresh(device)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("invalid_refresh_token"));
        }
        fx.login(PHONE, OLD_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
        fx.login(PHONE, NEW_PASSWORD).andExpect(status().isOk());
    }

    // R3
    @Test
    void aResetLinkStopsWorkingAfter24Hours() throws Exception {
        reset().andExpect(status().isOk());
        String token = fx.tokenFromLastEmail();
        fx.advance(Duration.ofHours(25));

        fx.acceptInvite(token, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_link"));
    }

    private ResultActions reset() throws Exception {
        return fx.admin(post(TRAINERS + "/" + ahmed.id() + "/reset-password"));
    }
}
