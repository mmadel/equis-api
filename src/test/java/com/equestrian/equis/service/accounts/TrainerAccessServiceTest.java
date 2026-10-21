package com.equestrian.equis.service.accounts;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.Member;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.assessment.MemberRepository;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import com.equestrian.equis.support.MutableClock;
import com.equestrian.equis.support.TestMembers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Disabling and enabling a trainer against a real database, with real tokens for the access cases.
// The container belongs to this class, so every test starts from an empty trainer table.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=disable-rules-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash"})
@AutoConfigureMockMvc
@Testcontainers
class TrainerAccessServiceTest {
    private static final String PASSWORD = "correct-horse-1";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    // Created-at columns use the real time and the database checks links and tokens expire after it,
    // so the clock starts at the real time.
    static final MutableClock CLOCK = new MutableClock(Instant.now());

    @TestBean
    Clock clock;

    static Clock clock() {
        return CLOCK;
    }

    @Autowired
    TrainerAccessService service;
    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @Autowired
    AuthTokenService authTokenService;
    @Autowired
    AccountLinkService accountLinkService;
    @Autowired
    PasswordService passwordService;
    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    RiderRepository riderRepository;
    @Autowired
    LevelRepository levelRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    AssignmentRepository assignmentRepository;
    @Autowired
    AssessmentBookingRepository bookingRepository;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        CLOCK.set(Instant.now().truncatedTo(ChronoUnit.MICROS));
        // Also drops the seeded trainers and sample riders.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
    }

    // Case 1
    @Test
    void disablesAnActiveTrainerWithNothingToConfirm() {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);

        Trainer result = service.disable(ahmed.getId(), false);

        assertThat(result.getStatus()).isEqualTo(TrainerStatus.DISABLED);
        assertThat(statusOf(ahmed)).isEqualTo("disabled");
    }

    // Case 2
    @Test
    void ridersNeedAConfirmAndNothingChangesWithoutIt() {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        riders(ahmed, 3);
        authTokenService.issue(Role.TRAINER, ahmed.getId(), false);
        accountLinkService.create(ahmed, InvitePurpose.RESET);

        assertThatThrownBy(() -> service.disable(ahmed.getId(), false))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.code()).isEqualTo("confirm_required");
                    assertThat(e.getMessage())
                            .isEqualTo("Ahmed Said has 3 riders and 0 upcoming assessments. Confirm to disable.");
                    assertThat(e.properties()).containsEntry("riderCount", 3L)
                            .containsEntry("upcomingAssessmentCount", 0L);
                });

        assertThat(statusOf(ahmed)).isEqualTo("active");
        assertThat(activeRefreshTokens(ahmed)).isEqualTo(1);
        assertThat(openLinks(ahmed)).isEqualTo(1);
    }

    // Case 3
    @Test
    void anUpcomingAssessmentNeedsAConfirm() {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        booking(ahmed, b -> { });

        assertThatThrownBy(() -> service.disable(ahmed.getId(), false))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("confirm_required");
                    assertThat(e.getMessage())
                            .isEqualTo("Ahmed Said has 0 riders and 1 upcoming assessment. Confirm to disable.");
                    assertThat(e.properties()).containsEntry("riderCount", 0L)
                            .containsEntry("upcomingAssessmentCount", 1L);
                });
        assertThat(statusOf(ahmed)).isEqualTo("active");
    }

    // Case 4
    @Test
    void onlyUpcomingBookingsNeedAConfirm() {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        booking(ahmed, b -> b.recordAttended(levelRepository.getReferenceById((short) 2), null, CLOCK.instant()));
        booking(ahmed, b -> b.recordNoShow(null, CLOCK.instant()));
        booking(ahmed, b -> b.cancel(CLOCK.instant()));

        assertThat(service.disable(ahmed.getId(), false).getStatus()).isEqualTo(TrainerStatus.DISABLED);
    }

    // Case 5
    @Test
    void confirmedDisableKeepsRidersAndTheirSeats() {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        List<Rider> riders = riders(ahmed, 3);
        seat(riders.get(0), ahmed, LocalDate.now(CLOCK).plusDays(7));

        assertThat(service.disable(ahmed.getId(), true).getStatus()).isEqualTo(TrainerStatus.DISABLED);

        assertThat(jdbc.queryForObject("select count(*) from rider where trainer_id = ?", Integer.class,
                ahmed.getId())).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from assignment where rider_id = ?", Integer.class,
                riders.get(0).getId())).isEqualTo(1);
    }

    // Case 6
    @Test
    void disablingRevokesEveryRefreshToken() {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        authTokenService.issue(Role.TRAINER, ahmed.getId(), false);
        authTokenService.issue(Role.TRAINER, ahmed.getId(), true);
        afterTokensWereCreated();

        service.disable(ahmed.getId(), false);

        assertThat(jdbc.queryForObject(
                "select count(*) from refresh_token where subject_id = ? and revoked_at is not null",
                Integer.class, ahmed.getId())).isEqualTo(2);
        assertThat(activeRefreshTokens(ahmed)).isZero();
    }

    // Case 7
    @Test
    void anAccessTokenFromBeforeTheDisableIsRefused() throws Exception {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        String bearer = "Bearer " + jwtService.issue(new AuthUser(ahmed.getId(), Role.TRAINER)).value();
        mvc.perform(get("/me/levels").header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isOk());

        service.disable(ahmed.getId(), false);

        mvc.perform(get("/me/levels").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    // Case 8
    @Test
    void afterDisableRefreshAndLoginAreRefused() throws Exception {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        String refresh = authTokenService.issue(Role.TRAINER, ahmed.getId(), false).refreshToken();
        afterTokensWereCreated();

        service.disable(ahmed.getId(), false);

        refresh(refresh)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_refresh_token"));
        login(ahmed)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("account_disabled"));
    }

    // Case 9
    @Test
    void anOpenResetLinkIsDeleted() throws Exception {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        String token = accountLinkService.create(ahmed, InvitePurpose.RESET);

        service.disable(ahmed.getId(), false);

        assertThat(openLinks(ahmed)).isZero();
        mvc.perform(post("/auth/invite/accept").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"token\": \"" + token + "\", \"password\": \"another-horse-2\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_link"));
    }

    // Case 10
    @Test
    void disablingTwiceChangesNothingTheSecondTime() {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        authTokenService.issue(Role.TRAINER, ahmed.getId(), false);
        afterTokensWereCreated();
        service.disable(ahmed.getId(), false);
        List<Timestamp> revokedBefore = revokedAt(ahmed);

        CLOCK.advance(Duration.ofMinutes(5));
        Trainer again = service.disable(ahmed.getId(), false);

        assertThat(again.getStatus()).isEqualTo(TrainerStatus.DISABLED);
        assertThat(revokedAt(ahmed)).isEqualTo(revokedBefore);
    }

    // Case 11
    @Test
    void anInvitedTrainerCannotBeDisabled() {
        Trainer mona = trainer("Mona Fathy", TrainerStatus.INVITED);

        assertInvalidStatus(() -> service.disable(mona.getId(), true), "invited", "disable");
        assertThat(statusOf(mona)).isEqualTo("invited");
    }

    // Case 12
    @Test
    void enablingLetsTheTrainerLogInAgainButOldTokensStayRevoked() throws Exception {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        String refresh = authTokenService.issue(Role.TRAINER, ahmed.getId(), false).refreshToken();
        afterTokensWereCreated();
        service.disable(ahmed.getId(), false);

        assertThat(service.enable(ahmed.getId()).getStatus()).isEqualTo(TrainerStatus.ACTIVE);

        login(ahmed).andExpect(status().isOk());
        refresh(refresh)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_refresh_token"));
    }

    // Case 13
    @Test
    void enableLeavesAnActiveTrainerAndRefusesAnInvitedOne() {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        Trainer mona = trainer("Mona Fathy", TrainerStatus.INVITED);

        assertThat(service.enable(ahmed.getId()).getStatus()).isEqualTo(TrainerStatus.ACTIVE);
        assertInvalidStatus(() -> service.enable(mona.getId()), "invited", "enable");
        assertThat(statusOf(mona)).isEqualTo("invited");
    }

    // Case 14
    @Test
    void ridersNeedReassignmentWhileTheirTrainerIsDisabledOrMissing() {
        Trainer active = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        Trainer leaving = trainer("Hany Gone", TrainerStatus.ACTIVE);
        rider("Carla", active);
        rider("Bassem", leaving);
        rider("Amira", null);
        service.disable(leaving.getId(), true);

        assertThat(riderRepository.findNeedingReassignment()).extracting(Rider::getName)
                .containsExactly("Amira", "Bassem");

        service.enable(leaving.getId());
        assertThat(riderRepository.findNeedingReassignment()).extracting(Rider::getName)
                .containsExactly("Amira");
    }

    // Case 15
    @Test
    void anUnknownTrainerIsNotFound() {
        UUID id = UUID.randomUUID();

        for (Runnable call : List.<Runnable>of(() -> service.disable(id, true), () -> service.enable(id))) {
            assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class, e -> {
                assertThat(e.status()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(e.code()).isEqualTo("trainer_not_found");
            });
        }
    }

    // created_at is stamped with the real time, and the database wants revoked_at after it.
    private static void afterTokensWereCreated() {
        CLOCK.set(Instant.now().plusSeconds(1).truncatedTo(ChronoUnit.MICROS));
    }

    private static void assertInvalidStatus(Runnable call, String status, String action) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(e.code()).isEqualTo("invalid_trainer_status");
            assertThat(e.properties()).containsEntry("status", status).containsEntry("action", action);
        });
    }

    private ResultActions login(Trainer trainer) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"login\": \"" + trainer.getPhone() + "\", \"password\": \"" + PASSWORD
                        + "\", \"rememberMe\": false }"));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"refreshToken\": \"" + refreshToken + "\" }"));
    }

    private String statusOf(Trainer trainer) {
        return jdbc.queryForObject("select status from trainer where id = ?", String.class, trainer.getId());
    }

    private int activeRefreshTokens(Trainer trainer) {
        return jdbc.queryForObject("select count(*) from refresh_token where subject_id = ? and revoked_at is null",
                Integer.class, trainer.getId());
    }

    private List<Timestamp> revokedAt(Trainer trainer) {
        return jdbc.queryForList("select revoked_at from refresh_token where subject_id = ? order by id",
                Timestamp.class, trainer.getId());
    }

    private int openLinks(Trainer trainer) {
        return jdbc.queryForObject("select count(*) from invite where trainer_id = ? and accepted_at is null",
                Integer.class, trainer.getId());
    }

    private Trainer trainer(String name, TrainerStatus status) {
        long n = jdbc.queryForObject("select count(*) from trainer", Long.class) + 1;
        Trainer trainer = new Trainer();
        trainer.setName(name);
        trainer.setPhone("0199999%04d".formatted(n));
        trainer.setEmail("trainer%d@school.eg".formatted(n));
        trainer.setStatus(status);
        trainer.setPasswordHash(status == TrainerStatus.INVITED ? null : passwordService.hash(PASSWORD));
        return trainerRepository.saveAndFlush(trainer);
    }

    private List<Rider> riders(Trainer trainer, int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> rider(trainer.getName() + " rider " + i, trainer))
                .toList();
    }

    private Rider rider(String name, Trainer trainer) {
        Rider rider = new Rider();
        rider.setName(name);
        rider.setLevel(levelRepository.getReferenceById((short) 1));
        rider.setTrainer(trainer);
        return new TransactionTemplate(transactionManager).execute(s -> riderRepository.save(rider));
    }

    private void seat(Rider rider, Trainer trainer, LocalDate date) {
        new TransactionTemplate(transactionManager).executeWithoutResult(s -> {
            Session session = sessionRepository.save(Session.of(template(date), date));
            assignmentRepository.save(Assignment.of(session, riderRepository.getReferenceById(rider.getId()),
                    trainer));
        });
    }

    private void booking(Trainer assessor, Consumer<AssessmentBooking> result) {
        new TransactionTemplate(transactionManager).executeWithoutResult(s -> {
            LocalDate date = LocalDate.of(2026, 10, 5);
            Session slot = sessionRepository.save(Session.of(template(date), date));
            Member member = memberRepository.save(TestMembers.member("Walk-in"));
            AssessmentBooking booking = AssessmentBooking.book(member, slot, assessor);
            result.accept(booking);
            bookingRepository.save(booking);
        });
    }

    private SessionTemplate template(LocalDate date) {
        SessionTemplate template = new SessionTemplate();
        template.setDayOfWeek(date.getDayOfWeek());
        template.setStartTime(LocalTime.of(16, 0));
        template.setEndTime(LocalTime.of(17, 0));
        template.setCapacity(4);
        template.setValidFrom(date.minusDays(30));
        return sessionTemplateRepository.save(template);
    }
}
