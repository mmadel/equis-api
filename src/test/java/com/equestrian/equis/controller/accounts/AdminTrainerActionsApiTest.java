package com.equestrian.equis.controller.accounts;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.service.accounts.AccountLinkCreatedEvent;
import com.equestrian.equis.service.accounts.AuthTokenService;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.service.accounts.PasswordService;
import com.equestrian.equis.support.MutableClock;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The W4 row actions over HTTP against a real database. The disable rules themselves are tested in
// TrainerAccessServiceTest; here one case per code is enough.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=trainer-actions-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash"})
@AutoConfigureMockMvc
@RecordApplicationEvents
class AdminTrainerActionsApiTest {
    private static final String PATH = "/admin/trainers";
    private static final String PASSWORD = "correct-horse-1";
    private static final String MONA = """
            { "name": "Mona Fathy", "phone": "01012345678", "email": "mona@example.com" }""";

    // Each action and the request it makes for a trainer id.
    private static final List<Function<String, MockHttpServletRequestBuilder>> ACTIONS = List.of(
            id -> post(PATH + "/" + id + "/resend-invite"),
            id -> post(PATH + "/" + id + "/reset-password"),
            id -> post(PATH + "/" + id + "/disable"),
            id -> post(PATH + "/" + id + "/enable"),
            id -> delete(PATH + "/" + id + "/invite"));

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    // Created-at columns use the real time and the database checks links and tokens expire after it,
    // so the clock starts at the real time.
    static final MutableClock CLOCK = new MutableClock(Instant.now());

    @TestBean
    Clock clock;

    static Clock clock() {
        return CLOCK;
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @Autowired
    AuthTokenService authTokenService;
    @Autowired
    PasswordService passwordService;
    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    RiderRepository riderRepository;
    @Autowired
    LevelRepository levelRepository;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    ApplicationEvents events;

    @BeforeEach
    void setUp() {
        CLOCK.set(Instant.now().truncatedTo(ChronoUnit.MICROS));
        // Also drops the seeded trainers and sample riders.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
    }

    // Case 1
    @Test
    void resendGivesAnInvitedTrainerANewLinkAndKillsTheOldOne() throws Exception {
        String id = idOf(invite(MONA).andExpect(status().isCreated()));
        String oldToken = linkEvents().getFirst().rawToken();
        events.clear();
        CLOCK.advance(Duration.ofHours(1));

        action(post(PATH + "/" + id + "/resend-invite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("invited"))
                .andExpect(jsonPath("$.pendingLink.purpose").value("invite"))
                .andExpect(jsonPath("$.pendingLink.expiresAt").value(CLOCK.instant().plus(Duration.ofHours(72)).toString()));

        assertThat(linkEvents()).singleElement()
                .satisfies(e -> assertThat(e.purpose()).isEqualTo(InvitePurpose.INVITE));
        accept(oldToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_link"));
    }

    // Case 2
    @Test
    void resendIsRefusedForAnActiveTrainer() throws Exception {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);

        action(post(PATH + "/" + ahmed.getId() + "/resend-invite"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_trainer_status"))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.action").value("resend-invite"));
        assertThat(linkEvents()).isEmpty();
    }

    // Case 3
    @Test
    void resetPasswordSendsALinkAndChangesNothingElse() throws Exception {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        authTokenService.issue(Role.TRAINER, ahmed.getId(), false);

        action(post(PATH + "/" + ahmed.getId() + "/reset-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.pendingLink.purpose").value("reset"))
                .andExpect(jsonPath("$.pendingLink.expiresAt").value(CLOCK.instant().plus(Duration.ofHours(24)).toString()));

        assertThat(linkEvents()).singleElement()
                .satisfies(e -> assertThat(e.purpose()).isEqualTo(InvitePurpose.RESET));
        assertThat(activeRefreshTokens(ahmed)).isEqualTo(1);
        login(ahmed).andExpect(status().isOk());
    }

    // Case 4
    @Test
    void resetPasswordIsRefusedForInvitedAndDisabledTrainers() throws Exception {
        Trainer mona = trainer("Mona Fathy", TrainerStatus.INVITED);
        Trainer hany = trainer("Hany Gone", TrainerStatus.DISABLED);

        for (Trainer t : List.of(mona, hany)) {
            action(post(PATH + "/" + t.getId() + "/reset-password"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("invalid_trainer_status"))
                    .andExpect(jsonPath("$.action").value("reset-password"));
        }
        assertThat(linkEvents()).isEmpty();
    }

    // Case 5
    @Test
    void disablingATrainerWithRidersNeedsAConfirm() throws Exception {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        rider("Carla", ahmed);

        action(post(PATH + "/" + ahmed.getId() + "/disable"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("confirm_required"))
                .andExpect(jsonPath("$.riderCount").value(1))
                .andExpect(jsonPath("$.upcomingAssessmentCount").value(0));
        assertThat(statusOf(ahmed)).isEqualTo("active");
    }

    // Case 6
    @Test
    void confirmedDisableReturnsTheRowAndARepeatChangesNothing() throws Exception {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        rider("Carla", ahmed);

        action(post(PATH + "/" + ahmed.getId() + "/disable").param("confirm", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("disabled"))
                .andExpect(jsonPath("$.riders.total").value(1));
        action(post(PATH + "/" + ahmed.getId() + "/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("disabled"));
    }

    // Case 7
    @Test
    void enableReactivatesADisabledTrainerAndRefusesAnInvitedOne() throws Exception {
        Trainer hany = trainer("Hany Gone", TrainerStatus.DISABLED);
        Trainer mona = trainer("Mona Fathy", TrainerStatus.INVITED);

        action(post(PATH + "/" + hany.getId() + "/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));
        action(post(PATH + "/" + mona.getId() + "/enable"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_trainer_status"));
    }

    // Case 8
    @Test
    void cancellingAnInviteRemovesTheTrainerSoTheyCanBeInvitedAgain() throws Exception {
        String id = idOf(invite(MONA).andExpect(status().isCreated()));

        action(delete(PATH + "/" + id + "/invite")).andExpect(status().isNoContent());

        assertThat(count("trainer")).isZero();
        assertThat(count("invite")).isZero();
        invite(MONA).andExpect(status().isCreated());
    }

    // Case 9
    @Test
    void anActiveTrainersInviteCannotBeCancelled() throws Exception {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);

        action(delete(PATH + "/" + ahmed.getId() + "/invite"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_trainer_status"))
                .andExpect(jsonPath("$.action").value("cancel-invite"));
        assertThat(count("trainer")).isEqualTo(1);
    }

    // Case 10
    @Test
    void anInvitedTrainerWithARiderCannotBeCancelled() throws Exception {
        String id = idOf(invite(MONA).andExpect(status().isCreated()));
        rider("Carla", trainerRepository.findById(UUID.fromString(id)).orElseThrow());

        action(delete(PATH + "/" + id + "/invite"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("trainer_in_use"));
        assertThat(count("trainer")).isEqualTo(1);
        assertThat(count("invite")).isEqualTo(1);
    }

    // Case 11
    @Test
    void anUnknownIdIsNotFoundAndANonUuidIsABadRequest() throws Exception {
        String unknown = UUID.randomUUID().toString();
        for (Function<String, MockHttpServletRequestBuilder> request : ACTIONS) {
            action(request.apply(unknown))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("trainer_not_found"));
            action(request.apply("abc")).andExpect(status().isBadRequest());
        }
    }

    // Case 12
    @Test
    void aTrainerTokenIsForbiddenOnEveryAction() throws Exception {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        String asTrainer = "Bearer " + jwtService.issue(new AuthUser(ahmed.getId(), Role.TRAINER)).value();

        for (Function<String, MockHttpServletRequestBuilder> request : ACTIONS) {
            mvc.perform(request.apply(ahmed.getId().toString()).header(HttpHeaders.AUTHORIZATION, asTrainer))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("forbidden"));
        }
        assertThat(statusOf(ahmed)).isEqualTo("active");
    }

    private ResultActions action(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(asAdmin(request));
    }

    private ResultActions invite(String body) throws Exception {
        return mvc.perform(asAdmin(post(PATH)).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions accept(String token) throws Exception {
        return mvc.perform(post("/auth/invite/accept").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"token\": \"" + token + "\", \"password\": \"another-horse-2\" }"));
    }

    private ResultActions login(Trainer trainer) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"login\": \"" + trainer.getPhone() + "\", \"password\": \"" + PASSWORD
                        + "\", \"rememberMe\": false }"));
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtService.issue(new AuthUser(UUID.randomUUID(), Role.ADMIN)).value());
    }

    private List<AccountLinkCreatedEvent> linkEvents() {
        return events.stream(AccountLinkCreatedEvent.class).toList();
    }

    private String statusOf(Trainer trainer) {
        return jdbc.queryForObject("select status from trainer where id = ?", String.class, trainer.getId());
    }

    private int activeRefreshTokens(Trainer trainer) {
        return jdbc.queryForObject("select count(*) from refresh_token where subject_id = ? and revoked_at is null",
                Integer.class, trainer.getId());
    }

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    private Trainer trainer(String name, TrainerStatus status) {
        long n = count("trainer") + 1;
        Trainer trainer = new Trainer();
        trainer.setName(name);
        trainer.setPhone("0199999%04d".formatted(n));
        trainer.setEmail("trainer%d@school.eg".formatted(n));
        trainer.setStatus(status);
        trainer.setPasswordHash(status == TrainerStatus.INVITED ? null : passwordService.hash(PASSWORD));
        return trainerRepository.saveAndFlush(trainer);
    }

    private void rider(String name, Trainer trainer) {
        new TransactionTemplate(transactionManager).executeWithoutResult(s -> {
            Rider rider = new Rider();
            rider.setName(name);
            rider.setLevel(levelRepository.getReferenceById((short) 1));
            rider.setTrainer(trainer);
            riderRepository.save(rider);
        });
    }

    private static String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
