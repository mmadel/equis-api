package com.equestrian.equis.controller.accounts;

import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.dto.accounts.TrainerRow;
import com.equestrian.equis.entity.accounts.TrainerStatus;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.equestrian.equis.support.TrainerAccountsFixture.START;
import static com.equestrian.equis.support.TrainerAccountsFixture.TRAINERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow W-E, access A1–A3: only the admin reaches /admin/trainers, and a refused request never sends an email.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Testcontainers
@Import(TrainerAccountsFixture.Config.class)
class TrainerAdminAccessTest {
    private static final String PASSWORD = "Ahmed@2026";

    // Every /admin/trainers endpoint for a trainer id.
    private static final List<Function<UUID, MockHttpServletRequestBuilder>> ENDPOINTS = List.of(
            id -> get(TRAINERS),
            id -> post(TRAINERS).contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"name\": \"Nour Ali\", \"phone\": \"01055556666\", \"email\": \"nour@example.com\" }"),
            id -> post(TRAINERS + "/" + id + "/resend-invite"),
            id -> post(TRAINERS + "/" + id + "/reset-password"),
            id -> post(TRAINERS + "/" + id + "/disable").param("confirm", "true"),
            id -> post(TRAINERS + "/" + id + "/enable"),
            id -> delete(TRAINERS + "/" + id + "/invite"));

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

    TrainerRow ahmed;
    TrainerRow mona;

    @BeforeEach
    void setUp() throws Exception {
        fx.reset();
        ahmed = fx.activeTrainer("Ahmed Said", "01022223333", "ahmed@example.com", PASSWORD);
        mona = fx.invite("Mona Fathy", "01012345678", "mona@example.com");
        fx.recorder().clear();
    }

    // A1. Each endpoint is tried on an active and an invited trainer, so every action would otherwise apply.
    @Test
    void everyEndpointNeedsAToken() throws Exception {
        List<Map<String, Object>> before = stored();

        for (Function<UUID, MockHttpServletRequestBuilder> endpoint : ENDPOINTS) {
            for (UUID id : List.of(ahmed.id(), mona.id())) {
                fx.perform(endpoint.apply(id))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value("unauthorized"));
            }
        }
        assertThat(stored()).isEqualTo(before);
        assertThat(fx.recorder().all()).isEmpty();
    }

    // A2
    @Test
    void aTrainerTokenIsForbiddenEverywhere() throws Exception {
        AuthTokens asAhmed = fx.loginTrainer("01022223333", PASSWORD);
        List<Map<String, Object>> before = stored();

        for (Function<UUID, MockHttpServletRequestBuilder> endpoint : ENDPOINTS) {
            for (UUID id : List.of(ahmed.id(), mona.id())) {
                fx.perform(fx.asTrainer(endpoint.apply(id), asAhmed))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value("forbidden"));
            }
        }
        assertThat(stored()).isEqualTo(before);
        assertThat(fx.statusOf(ahmed.id())).isEqualTo(TrainerStatus.ACTIVE);
        assertThat(fx.statusOf(mona.id())).isEqualTo(TrainerStatus.INVITED);
        assertThat(fx.recorder().all()).isEmpty();
    }

    // A3. Each refused invite or reset request is followed by one that works, which must add exactly one email.
    @Test
    void noEmailGoesOutForARequestThatEndsInAnError() throws Exception {
        TrainerRow hany = fx.activeTrainer("Hany Adel", "01033334444", "hany@example.com", PASSWORD);
        fx.admin(post(TRAINERS + "/" + hany.id() + "/disable")).andExpect(status().isOk());
        fx.recorder().clear();
        UUID unknown = UUID.randomUUID();

        List<ResultActions> refused = List.of(
                fx.inviteRequest("Ahmed Again", "01022223333", "ahmed.again@example.com"),         // phone_taken
                fx.inviteRequest("Ahmed Again", "01099990000", "ahmed@example.com"),               // email_taken
                fx.inviteRequest("Not The Admin", "01099990001", TrainerAccountsFixture.ADMIN_EMAIL), // email_taken
                fx.inviteRequest("", "123", "not-an-email"),                                        // validation
                fx.admin(post(TRAINERS + "/" + ahmed.id() + "/resend-invite")),                     // active
                fx.admin(post(TRAINERS + "/" + hany.id() + "/resend-invite")),                      // disabled
                fx.admin(post(TRAINERS + "/" + unknown + "/resend-invite")),                        // not found
                fx.admin(post(TRAINERS + "/" + mona.id() + "/reset-password")),                     // invited
                fx.admin(post(TRAINERS + "/" + hany.id() + "/reset-password")),                     // disabled
                fx.admin(post(TRAINERS + "/" + unknown + "/reset-password")));                      // not found
        for (ResultActions result : refused) {
            assertThat(result.andReturn().getResponse().getStatus()).isBetween(400, 499);
        }
        assertThat(fx.recorder().all()).isEmpty();

        fx.inviteRequest("Nour Ali", "01055556666", "nour@example.com").andExpect(status().isCreated());
        fx.admin(post(TRAINERS + "/" + mona.id() + "/resend-invite")).andExpect(status().isOk());
        fx.admin(post(TRAINERS + "/" + ahmed.id() + "/reset-password")).andExpect(status().isOk());
        assertThat(fx.recorder().all()).extracting(e -> e.to())
                .containsExactly("nour@example.com", "mona@example.com", "ahmed@example.com");
    }

    // Every trainer with its links, as stored. A resend or reset would change the token hash, a disable the status.
    private List<Map<String, Object>> stored() {
        return fx.jdbc().queryForList("""
                select t.id, t.status, t.updated_at, i.token_hash, i.purpose
                from trainer t left join invite i on i.trainer_id = t.id
                order by t.id, i.token_hash""");
    }
}
