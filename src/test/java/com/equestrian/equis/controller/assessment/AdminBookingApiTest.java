package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.support.MutableClock;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GET /admin/bookings (W2) over HTTP against a real database. Rows go in with SQL so each booking's
// created_at, status and result can be set exactly. The container belongs to this class.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=bookings-api-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash",
        "spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
public class AdminBookingApiTest {
    private static final String PATH = "/admin/bookings";
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // Friday 16 Oct 2026, 12:00 school time.
    private static final Instant FRIDAY_NOON = cairo(2026, 10, 16, 12, 0);

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    static final MutableClock CLOCK = new MutableClock(FRIDAY_NOON);

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
    JdbcTemplate jdbc;
    @Autowired
    EntityManagerFactory entityManagerFactory;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    SessionRepository sessionRepository;

    private UUID ahmed;
    private int seq;

    @BeforeEach
    void setUp() {
        CLOCK.set(FRIDAY_NOON);
        // Also drops the seeded trainers and sample riders.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        seq = 0;
        ahmed = trainer("Ahmed Said", "active");
    }

    // Case 1
    @Test
    void oneBookingPerStatusGivesEachStatusAndTotals() throws Exception {
        onePerStatus();

        list()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.upcoming").value(1))
                .andExpect(jsonPath("$.totals.waiting").value(1))
                .andExpect(jsonPath("$.totals.placed").value(1))
                .andExpect(jsonPath("$.totals.noshow").value(1))
                .andExpect(jsonPath("$.totals.cancelled").value(1))
                .andExpect(jsonPath("$.bookings.length()").value(5))
                .andExpect(jsonPath(row("Sara Ali", "status"), contains("upcoming")))
                .andExpect(jsonPath(row("Waleed Waiting", "status"), contains("waiting")))
                .andExpect(jsonPath(row("Pakinam Placed", "status"), contains("placed")))
                .andExpect(jsonPath(row("Nader Noshow", "status"), contains("noshow")))
                .andExpect(jsonPath(row("Camelia Cancelled", "status"), contains("cancelled")));
    }

    // Case 2
    @Test
    void aRebookedNoShowIsListedOnceByItsLatestBooking() throws Exception {
        UUID omar = member("Omar Hassan", "01222223333");
        booking(omar, slot(LocalDate.of(2026, 10, 10), LocalTime.of(10, 0)), ahmed, "noshow",
                cairo(2026, 10, 9, 9, 0), cairo(2026, 10, 10, 11, 0));
        UUID latest = booking(omar, slot(LocalDate.of(2026, 10, 24), LocalTime.of(10, 0)), ahmed, "upcoming",
                cairo(2026, 10, 11, 9, 0), null);

        list()
                .andExpect(jsonPath("$.bookings.length()").value(1))
                .andExpect(jsonPath("$.bookings[0].id").value(latest.toString()))
                .andExpect(jsonPath("$.bookings[0].slot.date").value("2026-10-24"))
                .andExpect(jsonPath("$.bookings[0].status").value("upcoming"))
                .andExpect(jsonPath("$.totals.noshow").value(0))
                .andExpect(jsonPath("$.totals.upcoming").value(1));
    }

    // Case 3
    @Test
    void aStatusFilterKeepsTheTotalsUnfiltered() throws Exception {
        onePerStatus();

        list("status", "waiting")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings.length()").value(1))
                .andExpect(jsonPath("$.bookings[0].member.name").value("Waleed Waiting"))
                .andExpect(jsonPath("$.totals.upcoming").value(1))
                .andExpect(jsonPath("$.totals.waiting").value(1))
                .andExpect(jsonPath("$.totals.placed").value(1))
                .andExpect(jsonPath("$.totals.noshow").value(1))
                .andExpect(jsonPath("$.totals.cancelled").value(1));
    }

    // Case 4
    @Test
    void anUnknownStatusIsAValidationFailure() throws Exception {
        list("status", "bogus")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("invalid_status"));
    }

    // Case 5
    @Test
    void searchFindsByPartOfTheNameOrPhone() throws Exception {
        onePerStatus();

        list("q", "SARA")
                .andExpect(jsonPath("$.bookings[*].member.name", contains("Sara Ali")))
                .andExpect(jsonPath("$.totals.placed").value(1));
        list("q", "010 1111")
                .andExpect(jsonPath("$.bookings[*].member.name", contains("Sara Ali")));
        list("q", "xyz")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings").isEmpty());
    }

    // Case 6
    @Test
    void aMemberWhosePhoneDoesNotHoldTheDigitsIsNotMatched() throws Exception {
        UUID nour = member("Nour", "01299998888");
        booking(nour, slot(LocalDate.of(2026, 10, 17), LocalTime.of(10, 0)), ahmed, "upcoming", null, null);

        list("q", "0101")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings").isEmpty())
                .andExpect(jsonPath("$.totals.upcoming").value(1));
    }

    // Case 7
    @Test
    void aPlacedRowCarriesTheResultAndTheRider() throws Exception {
        UUID pakinam = member("Pakinam Placed", "01033334444");
        Instant recordedAt = cairo(2026, 10, 15, 11, 0);
        booking(pakinam, slot(LocalDate.of(2026, 10, 15), LocalTime.of(10, 0)), ahmed, "placed",
                cairo(2026, 10, 14, 9, 0), recordedAt);
        UUID rider = rider(pakinam, "Pakinam Placed");

        list()
                .andExpect(jsonPath("$.bookings[0].status").value("placed"))
                .andExpect(jsonPath("$.bookings[0].levelId").value(2))
                .andExpect(jsonPath("$.bookings[0].recordedAt").value(recordedAt.toString()))
                .andExpect(jsonPath("$.bookings[0].riderId").value(rider.toString()))
                .andExpect(jsonPath("$.bookings[0].remindedAt").isEmpty())
                .andExpect(jsonPath("$.bookings[0].actions", contains("view_rider")));
    }

    // Case 8
    @Test
    void eachStatusHasItsActions() throws Exception {
        onePerStatus();

        list()
                .andExpect(jsonPath(row("Sara Ali", "actions") + "[*]",
                        contains("reschedule", "cancel", "change_assessor", "edit")))
                .andExpect(jsonPath(row("Waleed Waiting", "actions") + "[*]", contains("change_assessor", "remind", "edit")))
                .andExpect(jsonPath(row("Pakinam Placed", "actions") + "[*]", contains("view_rider")))
                .andExpect(jsonPath(row("Nader Noshow", "actions") + "[*]", contains("rebook", "change_assessor", "close", "edit")))
                .andExpect(jsonPath(row("Camelia Cancelled", "actions") + "[*]",
                        contains("rebook", "change_assessor", "close", "edit")));
    }

    // Case 9
    @Test
    void aDisabledAssessorCannotBeReminded() throws Exception {
        UUID hany = trainer("Hany Gone", "disabled");
        booking(member("Waleed Waiting", "01044445555"), slot(LocalDate.of(2026, 10, 16), LocalTime.of(10, 0)), hany,
                "upcoming", null, null);

        list()
                .andExpect(jsonPath("$.bookings[0].status").value("waiting"))
                .andExpect(jsonPath("$.bookings[0].assessor.name").value("Hany Gone"))
                .andExpect(jsonPath("$.bookings[0].assessor.status").value("disabled"))
                .andExpect(jsonPath("$.bookings[0].actions", contains("change_assessor", "edit")));
    }

    // Case 10
    @Test
    void sortsWaitingThenUpcomingThenNoShowAndCancelledThenPlaced() throws Exception {
        booking(member("P1 placed earlier", null), slot(LocalDate.of(2026, 10, 13), LocalTime.of(10, 0)), ahmed,
                "placed", null, cairo(2026, 10, 13, 11, 0));
        booking(member("P2 placed later", null), slot(LocalDate.of(2026, 10, 15), LocalTime.of(10, 0)), ahmed,
                "placed", null, cairo(2026, 10, 15, 11, 0));
        booking(member("N1 noshow 10 Oct", null), slot(LocalDate.of(2026, 10, 10), LocalTime.of(10, 0)), ahmed,
                "noshow", null, cairo(2026, 10, 10, 11, 0));
        booking(member("C1 cancelled 18 Oct", null), slot(LocalDate.of(2026, 10, 18), LocalTime.of(10, 0)), ahmed,
                "cancelled", null, null);
        booking(member("N2 noshow 12 Oct", null), slot(LocalDate.of(2026, 10, 12), LocalTime.of(10, 0)), ahmed,
                "noshow", null, cairo(2026, 10, 12, 11, 0));
        booking(member("U1 upcoming 20 Oct", null), slot(LocalDate.of(2026, 10, 20), LocalTime.of(10, 0)), ahmed,
                "upcoming", null, null);
        booking(member("U2 upcoming 17 Oct", null), slot(LocalDate.of(2026, 10, 17), LocalTime.of(10, 0)), ahmed,
                "upcoming", null, null);
        booking(member("W2 waiting 09:00", null), slot(LocalDate.of(2026, 10, 16), LocalTime.of(9, 0)), ahmed,
                "upcoming", null, null);
        booking(member("W1 waiting 08:00", null), slot(LocalDate.of(2026, 10, 16), LocalTime.of(8, 0)), ahmed,
                "upcoming", null, null);

        list().andExpect(jsonPath("$.bookings[*].member.name", contains(
                "W1 waiting 08:00", "W2 waiting 09:00",
                "U2 upcoming 17 Oct", "U1 upcoming 20 Oct",
                "C1 cancelled 18 Oct", "N2 noshow 12 Oct", "N1 noshow 10 Oct",
                "P2 placed later", "P1 placed earlier")));
    }

    // Case 11
    @Test
    void aBookingStartsWaitingWhenItsSlotStartsInCairo() throws Exception {
        booking(member("Sara Ali", "01011112222"), slot(LocalDate.of(2026, 10, 17), LocalTime.of(10, 0)), ahmed,
                "upcoming", null, null);
        CLOCK.set(cairo(2026, 10, 17, 9, 59));
        list().andExpect(jsonPath("$.bookings[0].status").value("upcoming"))
                .andExpect(jsonPath("$.bookings[0].slot.start").value("10:00"))
                .andExpect(jsonPath("$.bookings[0].slot.end").value("11:00"));

        CLOCK.set(cairo(2026, 10, 17, 10, 0));
        list().andExpect(jsonPath("$.bookings[0].status").value("waiting"));
    }

    // Case 12
    @Test
    void theListRunsTheSameNumberOfQueriesWhateverTheNumberOfBookings() throws Exception {
        UUID first = member("Member 00", "01100000000");
        booking(first, slot(LocalDate.of(2026, 10, 15), LocalTime.of(10, 0)), ahmed, "placed", null,
                cairo(2026, 10, 15, 11, 0));
        rider(first, "Member 00");
        long withOne = statementsFor(this::list);

        String[] statuses = {"upcoming", "placed", "noshow", "cancelled"};
        for (int i = 1; i < 20; i++) {
            UUID assessor = trainer("Trainer %02d".formatted(i), i % 3 == 0 ? "disabled" : "active");
            UUID m = member("Member %02d".formatted(i), "011%08d".formatted(i));
            String status = statuses[i % statuses.length];
            booking(m, slot(LocalDate.of(2026, 10, 10).plusDays(i), LocalTime.of(10, 0)), assessor, status, null,
                    status.equals("placed") || status.equals("noshow") ? cairo(2026, 10, 9, 11, 0) : null);
            if (status.equals("placed")) {
                rider(m, "Member %02d".formatted(i));
            } else if (i % 5 == 0) {
                // Some closed people too: the closed tab and totals cost no extra query.
                close(m, cairo(2026, 10, 15, i % 24, 0));
            }
        }
        // Members 10 and 15 are closed (5 is placed).
        list().andExpect(jsonPath("$.bookings.length()").value(18))
                .andExpect(jsonPath("$.totals.closed").value(2));
        long withTwenty = statementsFor(this::list);

        assertThat(withTwenty).isEqualTo(withOne);
        assertThat(statementsFor(() -> list("status", "closed"))).isEqualTo(withOne);
    }

    // Case 13
    @Test
    void onlyTheAdminGetsIn() throws Exception {
        String asTrainer = bearer(new AuthUser(ahmed, Role.TRAINER));

        mvc.perform(get(PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, asTrainer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    // ---- E11: email, person status, cancel reason, closed people ----

    // Case 14: Omar is a closed no-show. All leaves him out, and only totals.closed counts him.
    @Test
    void closedPeopleAreLeftOutOfAllAndTheFiveTotals() throws Exception {
        onePerStatus();
        UUID omar = member("Omar Closed", "01277778888");
        booking(omar, slot(LocalDate.of(2026, 10, 13), LocalTime.of(10, 0)), ahmed, "noshow", null,
                cairo(2026, 10, 13, 11, 0));
        close(omar, cairo(2026, 10, 15, 9, 0));

        list()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings.length()").value(5))
                .andExpect(jsonPath(row("Omar Closed", "id")).isEmpty())
                .andExpect(jsonPath("$.totals.upcoming").value(1))
                .andExpect(jsonPath("$.totals.waiting").value(1))
                .andExpect(jsonPath("$.totals.placed").value(1))
                .andExpect(jsonPath("$.totals.noshow").value(1))
                .andExpect(jsonPath("$.totals.cancelled").value(1))
                .andExpect(jsonPath("$.totals.closed").value(1));
        list("status", "noshow")
                .andExpect(jsonPath("$.bookings[*].member.name", contains("Nader Noshow")));
    }

    // Case 15
    @Test
    void theClosedTabListsOnlyClosedPeopleWithReopenAndEdit() throws Exception {
        onePerStatus();
        UUID omar = member("Omar Closed", "01277778888");
        booking(omar, slot(LocalDate.of(2026, 10, 13), LocalTime.of(10, 0)), ahmed, "noshow", null,
                cairo(2026, 10, 13, 11, 0));
        close(omar, cairo(2026, 10, 15, 9, 0));

        list("status", "closed")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings.length()").value(1))
                .andExpect(jsonPath("$.bookings[0].member.name").value("Omar Closed"))
                .andExpect(jsonPath("$.bookings[0].memberStatus").value("closed"))
                .andExpect(jsonPath("$.bookings[0].status").value("noshow"))
                .andExpect(jsonPath("$.bookings[0].actions", contains("reopen", "edit")))
                .andExpect(jsonPath("$.totals.closed").value(1))
                .andExpect(jsonPath("$.totals.noshow").value(1));
    }

    // Case 16: any part of the email, ignoring case. Every other member's email is on mail.eg.
    @Test
    void searchFindsByPartOfTheEmail() throws Exception {
        onePerStatus();
        UUID salma = member("Salma Mostafa", "01088889999", "salma.m@mail.com");
        booking(salma, slot(LocalDate.of(2026, 10, 19), LocalTime.of(10, 0)), ahmed, "upcoming", null, null);

        list("q", "MAIL.COM")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings[*].member.name", contains("Salma Mostafa")))
                .andExpect(jsonPath("$.bookings[0].member.email").value("salma.m@mail.com"));
        list("q", "a.m@")
                .andExpect(jsonPath("$.bookings[*].member.name", contains("Salma Mostafa")));
        list("q", "@mail.eg")
                .andExpect(jsonPath("$.bookings.length()").value(5));
    }

    // Case 17: Camelia's slot was deleted (no reason); the admin cancelled the other two.
    @Test
    void aCancelledRowCarriesTheAdminsReason() throws Exception {
        onePerStatus();
        UUID hala = member("Hala Unwell", "01212121212");
        cancelWithReason(booking(hala, slot(LocalDate.of(2026, 10, 19), LocalTime.of(10, 0)), ahmed, "cancelled",
                null, null), "person_unwell", null);
        UUID tarek = member("Tarek Other", "01213131313");
        cancelWithReason(booking(tarek, slot(LocalDate.of(2026, 10, 20), LocalTime.of(10, 0)), ahmed, "cancelled",
                null, null), "other", "Moved to Alexandria");

        list("status", "cancelled")
                .andExpect(jsonPath(row("Hala Unwell", "cancelReason"), contains("person_unwell")))
                .andExpect(jsonPath(row("Hala Unwell", "cancelReasonText"), contains((Object) null)))
                .andExpect(jsonPath(row("Tarek Other", "cancelReason"), contains("other")))
                .andExpect(jsonPath(row("Tarek Other", "cancelReasonText"), contains("Moved to Alexandria")))
                .andExpect(jsonPath(row("Camelia Cancelled", "cancelReason"), contains((Object) null)));
        list("status", "upcoming")
                .andExpect(jsonPath("$.bookings[0].cancelReason").isEmpty());
    }

    // Case 18
    @Test
    void eachRowCarriesThePersonsStatus() throws Exception {
        onePerStatus();

        list()
                .andExpect(jsonPath(row("Pakinam Placed", "memberStatus"), contains("rider")))
                .andExpect(jsonPath(row("Sara Ali", "memberStatus"), contains("pending_assessment")))
                .andExpect(jsonPath(row("Waleed Waiting", "memberStatus"), contains("pending_assessment")))
                .andExpect(jsonPath(row("Nader Noshow", "memberStatus"), contains("no_show")))
                .andExpect(jsonPath(row("Camelia Cancelled", "memberStatus"), contains("pending_assessment")));
    }

    // Case 19
    @Test
    void theMostRecentlyClosedComesFirst() throws Exception {
        UUID early = member("Early Closed", "01214141414");
        booking(early, slot(LocalDate.of(2026, 10, 12), LocalTime.of(10, 0)), ahmed, "noshow", null,
                cairo(2026, 10, 12, 11, 0));
        close(early, cairo(2026, 10, 13, 9, 0));
        UUID late = member("Late Closed", "01215151515");
        booking(late, slot(LocalDate.of(2026, 10, 18), LocalTime.of(10, 0)), ahmed, "upcoming", null, null);
        close(late, cairo(2026, 10, 15, 9, 0));

        list("status", "closed")
                .andExpect(jsonPath("$.bookings[*].member.name", contains("Late Closed", "Early Closed")))
                .andExpect(jsonPath("$.totals.closed").value(2))
                .andExpect(jsonPath("$.totals.upcoming").value(0));
        list("status", "closed").andExpect(jsonPath("$.bookings[0].status").value("upcoming"));
        list("q", "closed").andExpect(jsonPath("$.bookings").isEmpty());
    }

    // Case 20: the row only offers edit, and the actions it no longer offers are refused.
    @Test
    void aClosedPersonsRowCannotBeChangedOrReminded() throws Exception {
        UUID nader = member("Nader Closed", "01216161616");
        UUID noshow = booking(nader, slot(LocalDate.of(2026, 10, 14), LocalTime.of(10, 0)), ahmed, "noshow", null,
                cairo(2026, 10, 14, 11, 0));
        close(nader, cairo(2026, 10, 15, 9, 0));
        UUID waleed = member("Waleed Closed", "01217171717");
        UUID waiting = booking(waleed, slot(LocalDate.of(2026, 10, 16), LocalTime.of(10, 0)), ahmed, "upcoming",
                null, null);
        close(waleed, cairo(2026, 10, 15, 10, 0));
        UUID saturday = slot(LocalDate.of(2026, 10, 24), LocalTime.of(10, 0));

        mvc.perform(asAdmin(patch(PATH + "/" + noshow).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"slotId\": \"%s\" }".formatted(saturday))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("member_closed"));
        mvc.perform(asAdmin(post(PATH + "/" + waiting + "/remind")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("member_closed"));

        assertThat(jdbc.queryForObject("select count(*) from assessment_booking where member_id = ?", Long.class,
                nader)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from notice", Long.class)).isZero();
    }

    // Sara upcoming tomorrow, Waleed waiting since 10:00 today, Pakinam placed yesterday (with her rider),
    // Nader a no-show on Wednesday, Camelia's Sunday slot deleted.
    private void onePerStatus() {
        booking(member("Sara Ali", "01011112222"), slot(LocalDate.of(2026, 10, 17), LocalTime.of(10, 0)), ahmed,
                "upcoming", null, null);
        booking(member("Waleed Waiting", "01044445555"), slot(LocalDate.of(2026, 10, 16), LocalTime.of(10, 0)),
                ahmed, "upcoming", null, null);
        UUID pakinam = member("Pakinam Placed", "01033334444");
        booking(pakinam, slot(LocalDate.of(2026, 10, 15), LocalTime.of(10, 0)), ahmed, "placed", null,
                cairo(2026, 10, 15, 11, 0));
        rider(pakinam, "Pakinam Placed");
        booking(member("Nader Noshow", "01255556666"), slot(LocalDate.of(2026, 10, 14), LocalTime.of(10, 0)), ahmed,
                "noshow", null, cairo(2026, 10, 14, 11, 0));
        booking(member("Camelia Cancelled", "01566667777"), slot(LocalDate.of(2026, 10, 18), LocalTime.of(10, 0)),
                ahmed, "cancelled", null, null);
    }

    private ResultActions list() throws Exception {
        return mvc.perform(asAdmin(get(PATH)));
    }

    private ResultActions list(String param, String value) throws Exception {
        return mvc.perform(asAdmin(get(PATH).param(param, value)));
    }

    private static String row(String memberName, String field) {
        return "$.bookings[?(@.member.name == '" + memberName + "')]." + field;
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

    private static Instant cairo(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, CAIRO).toInstant();
    }

    private UUID trainer(String name, String status) {
        seq++;
        return jdbc.queryForObject("""
                        insert into trainer (name, phone, email, status, password_hash)
                        values (?, ?, ?, ?, ?) returning id""",
                UUID.class, name, "0199999%04d".formatted(seq), "trainer%d@school.eg".formatted(seq), status,
                status.equals("invited") ? null : "not-a-real-hash");
    }

    // Every member needs a phone and an email; a null phone gets a made-up unique one.
    private UUID member(String name, String phone) {
        return member(name, phone, null);
    }

    private UUID member(String name, String phone, String email) {
        seq++;
        return jdbc.queryForObject("insert into member (name, phone, email) values (?, ?, ?) returning id",
                UUID.class, name, phone != null ? phone : "0155555%04d".formatted(seq),
                email != null ? email : "member%d@mail.eg".formatted(seq));
    }

    private void close(UUID member, Instant at) {
        jdbc.update("update member set closed_at = ? where id = ?", Timestamp.from(at), member);
    }

    // The admin's cancel, as cancelWithReason stores it.
    private void cancelWithReason(UUID booking, String reason, String text) {
        jdbc.update("update assessment_booking set cancel_reason = ?, cancel_reason_text = ? where id = ?",
                reason, text, booking);
    }

    // An assessment slot (no level) on that date, one hour long. Saved through JPA, as the app saves them,
    // so the start and end times are stored the way the app reads them.
    private UUID slot(LocalDate date, LocalTime start) {
        return new TransactionTemplate(transactionManager).execute(s -> {
            SessionTemplate template = new SessionTemplate();
            template.setDayOfWeek(date.getDayOfWeek());
            template.setStartTime(start);
            template.setEndTime(start.plusHours(1));
            template.setCapacity(6);
            template.setValidFrom(date);
            template = sessionTemplateRepository.save(template);
            return sessionRepository.save(Session.of(template, date)).getId();
        });
    }

    // The result columns follow the status as the table's check requires; placed members go to level 2.
    // A null createdAt leaves the database default.
    private UUID booking(UUID member, UUID slot, UUID assessor, String status, Instant createdAt, Instant recordedAt) {
        Boolean attended = switch (status) {
            case "placed" -> true;
            case "noshow" -> false;
            default -> null;
        };
        Timestamp cancelledAt = status.equals("cancelled") ? Timestamp.from(FRIDAY_NOON) : null;
        Timestamp created = Timestamp.from(createdAt != null ? createdAt : Instant.now());
        return jdbc.queryForObject("""
                        insert into assessment_booking
                            (member_id, session_id, assessor_id, status, attended, level_id, recorded_at,
                             cancelled_at, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?) returning id""",
                UUID.class, member, slot, assessor, status, attended, status.equals("placed") ? 2 : null,
                recordedAt == null ? null : Timestamp.from(recordedAt), cancelledAt, created);
    }

    private UUID rider(UUID member, String name) {
        return jdbc.queryForObject(
                "insert into rider (name, level_id, trainer_id, member_id) values (?, 2, ?, ?) returning id",
                UUID.class, name, ahmed, member);
    }

    @FunctionalInterface
    private interface Request {
        ResultActions run() throws Exception;
    }
}
