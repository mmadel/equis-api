package com.equestrian.equis.controller.accounts;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.Member;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.assessment.MemberRepository;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import com.equestrian.equis.service.accounts.AccountLinkCreatedEvent;
import com.equestrian.equis.service.accounts.AuthTokenService;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.service.accounts.TokenGenerator;
import com.equestrian.equis.support.MutableClock;
import com.equestrian.equis.support.TestMembers;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GET and POST /admin/trainers over HTTP against a real database. The container belongs to this class,
// so every test starts from an empty trainer table.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=" + AdminTrainerApiTest.ADMIN_EMAIL,
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash",
        "spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@Testcontainers
@RecordApplicationEvents
public class AdminTrainerApiTest {
    static final String ADMIN_EMAIL = "trainers-api-admin@example.com";
    private static final String PATH = "/admin/trainers";
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // Friday 16 Oct 2026, 10:00 school time: used where the calendar month matters.
    private static final Instant OCTOBER = ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, CAIRO).toInstant();
    private static final String MONA = """
            { "name": "Mona Fathy", "phone": "01012345678", "email": "mona@example.com" }""";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    // Created-at columns use the real time and the database checks links and tokens expire after it,
    // so the clock starts at the real time unless a test needs a given month.
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
    TokenGenerator tokenGenerator;
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
    AssessmentBookingRepository bookingRepository;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    EntityManagerFactory entityManagerFactory;
    @Autowired
    ApplicationEvents events;

    @BeforeEach
    void setUp() {
        CLOCK.set(Instant.now().truncatedTo(ChronoUnit.MICROS));
        // Also drops the seeded trainers and sample riders, so totals start at zero.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
    }

    // Case 1
    @Test
    void listsTotalsAndRowsActiveThenInvitedThenDisabled() throws Exception {
        trainer("Zeina Disabled", TrainerStatus.DISABLED);
        trainer("Omar Invited", TrainerStatus.INVITED);
        trainer("Ahmed Active", TrainerStatus.ACTIVE);

        list()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.active").value(1))
                .andExpect(jsonPath("$.totals.invited").value(1))
                .andExpect(jsonPath("$.totals.disabled").value(1))
                .andExpect(jsonPath("$.totals.ridersCovered").value(0))
                .andExpect(jsonPath("$.trainers.length()").value(3))
                .andExpect(jsonPath("$.trainers[0].name").value("Ahmed Active"))
                .andExpect(jsonPath("$.trainers[0].status").value("active"))
                .andExpect(jsonPath("$.trainers[1].status").value("invited"))
                .andExpect(jsonPath("$.trainers[2].status").value("disabled"));
    }

    // Case 2
    @Test
    void countsRidersByLevel() throws Exception {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        riders(ahmed, (short) 4, 6);
        riders(ahmed, (short) 1, 3);

        list()
                .andExpect(jsonPath("$.trainers[0].riders.total").value(9))
                .andExpect(jsonPath("$.trainers[0].riders.byLevel.length()").value(2))
                .andExpect(jsonPath("$.trainers[0].riders.byLevel[0].levelId").value(1))
                .andExpect(jsonPath("$.trainers[0].riders.byLevel[0].count").value(3))
                .andExpect(jsonPath("$.trainers[0].riders.byLevel[1].levelId").value(4))
                .andExpect(jsonPath("$.trainers[0].riders.byLevel[1].count").value(6));
    }

    // Case 3
    @Test
    void ridersCoveredCountsOnlyActiveTrainersRiders() throws Exception {
        riders(trainer("Ahmed Said", TrainerStatus.ACTIVE), (short) 2, 4);
        riders(trainer("Hany Gone", TrainerStatus.DISABLED), (short) 2, 5);

        list()
                .andExpect(jsonPath("$.totals.ridersCovered").value(4))
                .andExpect(jsonPath("$.trainers[1].riders.total").value(5));
    }

    // Case 4
    @Test
    void assessmentsThisMonthCountsPlacedAndNoShowRecordedThisMonth() throws Exception {
        CLOCK.set(OCTOBER);
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        placed(ahmed, cairo(2026, 10, 5, 17, 0));
        noShow(ahmed, cairo(2026, 10, 12, 17, 0));
        placed(ahmed, cairo(2026, 9, 30, 17, 0));
        upcoming(ahmed);

        list().andExpect(jsonPath("$.trainers[0].assessmentsThisMonth").value(2));
    }

    // Case 5
    @Test
    void theMonthEndsAtMidnightSchoolTime() throws Exception {
        CLOCK.set(OCTOBER);
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        Trainer mona = trainer("Mona Fathy", TrainerStatus.ACTIVE);
        placed(ahmed, cairo(2026, 10, 31, 23, 30));
        placed(mona, cairo(2026, 11, 1, 0, 10));

        list()
                .andExpect(jsonPath("$.trainers[0].name").value("Ahmed Said"))
                .andExpect(jsonPath("$.trainers[0].assessmentsThisMonth").value(1))
                .andExpect(jsonPath("$.trainers[1].assessmentsThisMonth").value(0));
    }

    // Case 6
    @Test
    void anInvitedTrainersLinkShowsWhetherItExpired() throws Exception {
        invite(MONA).andExpect(status().isCreated());

        list()
                .andExpect(jsonPath("$.totals.invited").value(1))
                .andExpect(jsonPath("$.trainers[0].pendingLink.purpose").value("invite"))
                .andExpect(jsonPath("$.trainers[0].pendingLink.expired").value(false));

        CLOCK.advance(Duration.ofHours(73));
        list()
                .andExpect(jsonPath("$.totals.invited").value(1))
                .andExpect(jsonPath("$.trainers[0].pendingLink.expired").value(true));
    }

    // Case 7
    @Test
    void theListRunsTheSameNumberOfQueriesWhateverTheNumberOfTrainers() throws Exception {
        Trainer first = trainer("Trainer 00", TrainerStatus.ACTIVE);
        riders(first, (short) 1, 1);
        placed(first, CLOCK.instant().minusSeconds(60));
        long withOne = statementsFor(this::list);

        for (int i = 1; i < 10; i++) {
            Trainer t = trainer("Trainer %02d".formatted(i), i % 2 == 0 ? TrainerStatus.ACTIVE : TrainerStatus.INVITED);
            riders(t, (short) (1 + i % 6), 2);
            placed(t, CLOCK.instant().minusSeconds(60));
        }
        long withTen = statementsFor(this::list);

        assertThat(withTen).isEqualTo(withOne);
    }

    // Case 8
    @Test
    void invitingSavesAnInvitedTrainerWithOneOpenLinkAndPublishesItOnce() throws Exception {
        invite(MONA)
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.name").value("Mona Fathy"))
                .andExpect(jsonPath("$.status").value("invited"))
                .andExpect(jsonPath("$.riders.total").value(0))
                .andExpect(jsonPath("$.riders.byLevel").isEmpty())
                .andExpect(jsonPath("$.assessmentsThisMonth").value(0))
                .andExpect(jsonPath("$.lastActiveAt").isEmpty())
                .andExpect(jsonPath("$.pendingLink.purpose").value("invite"))
                .andExpect(jsonPath("$.pendingLink.expired").value(false));

        Map<String, Object> trainer = jdbc.queryForMap(
                "select id, status, password_hash from trainer where phone = '01012345678'");
        assertThat(trainer.get("status")).isEqualTo("invited");
        assertThat(trainer.get("password_hash")).isNull();
        List<Map<String, Object>> links = jdbc.queryForList(
                "select token_hash, purpose, expires_at from invite where trainer_id = ? and accepted_at is null",
                trainer.get("id"));
        assertThat(links).hasSize(1);
        assertThat(links.getFirst().get("purpose")).isEqualTo("invite");
        Instant expiresAt = ((Timestamp) links.getFirst().get("expires_at")).toInstant();
        assertThat(expiresAt).isEqualTo(CLOCK.instant().plus(Duration.ofHours(72)));

        List<AccountLinkCreatedEvent> published = events.stream(AccountLinkCreatedEvent.class).toList();
        assertThat(published).hasSize(1);
        AccountLinkCreatedEvent event = published.getFirst();
        assertThat(event.trainerId()).isEqualTo(trainer.get("id"));
        assertThat(event.email()).isEqualTo("mona@example.com");
        assertThat(event.purpose()).isEqualTo(InvitePurpose.INVITE);
        assertThat(event.expiresAt()).isEqualTo(expiresAt);
        assertThat(tokenGenerator.hash(event.rawToken())).isEqualTo(links.getFirst().get("token_hash"));
    }

    // Case 9
    @Test
    void theInputIsTidiedBeforeItIsStored() throws Exception {
        invite("""
                { "name": " Mona ", "phone": "010 1234 5678", "email": "Mona@Example.COM" }""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mona"))
                .andExpect(jsonPath("$.phone").value("01012345678"))
                .andExpect(jsonPath("$.email").value("mona@example.com"));

        Map<String, Object> stored = jdbc.queryForMap("select name, phone, email from trainer");
        assertThat(stored).containsEntry("name", "Mona")
                .containsEntry("phone", "01012345678")
                .containsEntry("email", "mona@example.com");
    }

    // Case 10
    @Test
    void aWrongShapeIsAValidationFailure() throws Exception {
        invite("""
                { "name": "  ", "phone": "12345", "email": "nope" }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("name", "phone", "email")));

        assertThat(trainerCount()).isZero();
        assertThat(events.stream(AccountLinkCreatedEvent.class)).isEmpty();
    }

    // Case 11
    @Test
    void anotherTrainersPhoneIsTaken() throws Exception {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);

        invite("""
                { "name": "Mona Fathy", "phone": "%s", "email": "mona@example.com" }""".formatted(ahmed.getPhone()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("phone_taken"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("phone"));

        assertThat(trainerCount()).isEqualTo(1);
        assertThat(events.stream(AccountLinkCreatedEvent.class)).isEmpty();
    }

    // Case 12
    @Test
    void aTrainersOrTheAdminsEmailIsTakenWithTheSameAnswer() throws Exception {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);

        String trainerClash = invite("""
                { "name": "Mona Fathy", "phone": "01012345678", "email": "%s" }"""
                .formatted(ahmed.getEmail().toUpperCase()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_taken"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
                .andReturn().getResponse().getContentAsString();
        String adminClash = invite("""
                { "name": "Mona Fathy", "phone": "01012345678", "email": "%s" }""".formatted(ADMIN_EMAIL))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_taken"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
                .andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(adminClash, "$.detail"))
                .isEqualTo(JsonPath.read(trainerClash, "$.detail"));
        assertThat(trainerCount()).isEqualTo(1);
        assertThat(events.stream(AccountLinkCreatedEvent.class)).isEmpty();
    }

    // Case 13
    @Test
    void onlyTheAdminGetsIn() throws Exception {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        String asTrainer = bearer(new AuthUser(ahmed.getId(), Role.TRAINER));

        mvc.perform(get(PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(MONA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, asTrainer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        mvc.perform(post(PATH).header(HttpHeaders.AUTHORIZATION, asTrainer)
                        .contentType(MediaType.APPLICATION_JSON).content(MONA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));

        assertThat(trainerCount()).isEqualTo(1);
    }

    // Case 14
    @Test
    void aTokenRefreshMarksTheTrainerActive() throws Exception {
        Trainer ahmed = trainer("Ahmed Said", TrainerStatus.ACTIVE);
        String refresh = authTokenService.issue(Role.TRAINER, ahmed.getId(), true).refreshToken();
        CLOCK.advance(Duration.ofMinutes(20));

        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"refreshToken\": \"" + refresh + "\" }"))
                .andExpect(status().isOk());

        Timestamp lastActive = jdbc.queryForObject("select last_active_at from trainer where id = ?",
                Timestamp.class, ahmed.getId());
        assertThat(lastActive.toInstant()).isEqualTo(CLOCK.instant());
        list().andExpect(jsonPath("$.trainers[0].lastActiveAt").value(CLOCK.instant().toString()));
    }

    // Case 15
    @Test
    void theEventNeverPrintsTheToken() {
        AccountLinkCreatedEvent event = new AccountLinkCreatedEvent(UUID.randomUUID(), "Mona Fathy",
                "mona@example.com", InvitePurpose.INVITE, "raw-secret-token-123", CLOCK.instant());

        assertThat(event.toString()).doesNotContain("raw-secret-token-123").contains("INVITE");
    }

    private ResultActions list() throws Exception {
        return mvc.perform(asAdmin(get(PATH)));
    }

    private ResultActions invite(String body) throws Exception {
        return mvc.perform(asAdmin(post(PATH)).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long statementsFor(Request request) throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        request.run().andExpect(status().isOk());
        return statistics.getPrepareStatementCount();
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(UUID.randomUUID(), Role.ADMIN)));
    }

    private String bearer(AuthUser user) {
        return "Bearer " + jwtService.issue(user).value();
    }

    private int trainerCount() {
        return jdbc.queryForObject("select count(*) from trainer", Integer.class);
    }

    private static Instant cairo(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, CAIRO).toInstant();
    }

    private Trainer trainer(String name, TrainerStatus status) {
        long n = trainerCount() + 1;
        Trainer trainer = new Trainer();
        trainer.setName(name);
        trainer.setPhone("0199999%04d".formatted(n));
        trainer.setEmail("trainer%d@school.eg".formatted(n));
        trainer.setStatus(status);
        trainer.setPasswordHash(status == TrainerStatus.INVITED ? null : "not-a-real-hash");
        return trainerRepository.saveAndFlush(trainer);
    }

    private void riders(Trainer trainer, short levelId, int count) {
        new TransactionTemplate(transactionManager).executeWithoutResult(s -> {
            for (int i = 0; i < count; i++) {
                Rider rider = new Rider();
                rider.setName(trainer.getName() + " rider " + levelId + "-" + i);
                rider.setLevel(levelRepository.getReferenceById(levelId));
                rider.setTrainer(trainer);
                riderRepository.save(rider);
            }
        });
    }

    private void placed(Trainer assessor, Instant recordedAt) {
        booking(assessor, b -> b.recordAttended(levelRepository.getReferenceById((short) 2), null, recordedAt));
    }

    private void noShow(Trainer assessor, Instant recordedAt) {
        booking(assessor, b -> b.recordNoShow(null, recordedAt));
    }

    private void upcoming(Trainer assessor) {
        booking(assessor, b -> { });
    }

    private void booking(Trainer assessor, java.util.function.Consumer<AssessmentBooking> result) {
        new TransactionTemplate(transactionManager).executeWithoutResult(s -> {
            SessionTemplate template = new SessionTemplate();
            template.setDayOfWeek(LocalDate.of(2026, 10, 5).getDayOfWeek());
            template.setStartTime(LocalTime.of(16, 0));
            template.setEndTime(LocalTime.of(17, 0));
            template.setCapacity(4);
            template.setValidFrom(LocalDate.of(2026, 9, 1));
            template = sessionTemplateRepository.save(template);
            Session slot = sessionRepository.save(Session.of(template, LocalDate.of(2026, 10, 5)));
            Member member = memberRepository.save(TestMembers.member("Walk-in"));
            AssessmentBooking booking = AssessmentBooking.book(member, slot, assessor);
            result.accept(booking);
            bookingRepository.save(booking);
        });
    }

    @FunctionalInterface
    private interface Request {
        ResultActions run() throws Exception;
    }
}
