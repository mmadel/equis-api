package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.assessment.AdminNewMemberRequest;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.service.assessment.BookingAction;
import com.equestrian.equis.service.assessment.BookingActions;
import com.equestrian.equis.service.assessment.MemberBookingService;
import com.equestrian.equis.support.MutableClock;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// PATCH /admin/bookings/{id} (W2: reschedule, rebook, change assessor) over HTTP against a real database.
// Not @Transactional, so the race cases run in real, concurrent transactions. The container belongs to this class.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=booking-changes-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash"})
@AutoConfigureMockMvc
@Testcontainers
public class AdminBookingChangeApiTest {
    private static final String PATH = "/admin/bookings/";
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // Friday 16 Oct 2026, 12:00 school time.
    private static final Instant FRIDAY_NOON = cairo(2026, 10, 16, 12, 0);
    private static final LocalDate FRI_16 = LocalDate.of(2026, 10, 16);
    private static final LocalDate SAT_17 = LocalDate.of(2026, 10, 17);
    private static final LocalDate TUE_20 = LocalDate.of(2026, 10, 20);
    private static final LocalDate SAT_24 = LocalDate.of(2026, 10, 24);
    private static final LocalTime TEN = LocalTime.of(10, 0);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

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
    PlatformTransactionManager transactionManager;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    LevelRepository levelRepository;
    @Autowired
    MemberBookingService memberBooking;

    private UUID ahmed;
    private UUID mona;
    private int seq;

    @BeforeEach
    void setUp() {
        CLOCK.set(FRIDAY_NOON);
        // Also drops the seeded trainers and sample riders.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        seq = 0;
        ahmed = trainer("Ahmed Said", "active");
        mona = trainer("Mona Kamal", "active");
    }

    // Case 1
    @Test
    void rescheduleMovesTheSameBookingAndItsSeat() throws Exception {
        UUID saturday = slot(SAT_17, TEN, 6);
        UUID tuesday = slot(TUE_20, LocalTime.of(17, 0), 4);
        UUID sara = booking(member("Sara Ali"), saturday, ahmed, "upcoming");
        booking(member("Other One"), tuesday, ahmed, "upcoming");
        booking(member("Other Two"), tuesday, ahmed, "upcoming");

        change(sara, "{ \"slotId\": \"%s\" }".formatted(tuesday))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sara.toString()))
                .andExpect(jsonPath("$.slot.id").value(tuesday.toString()))
                .andExpect(jsonPath("$.slot.date").value("2026-10-20"))
                .andExpect(jsonPath("$.slot.start").value("17:00"))
                .andExpect(jsonPath("$.status").value("upcoming"))
                .andExpect(jsonPath("$.assessor.id").value(ahmed.toString()));

        assertThat(upcomingIn(saturday)).isZero();
        assertThat(upcomingIn(tuesday)).isEqualTo(3);
        assertThat(sessionOf(sara)).isEqualTo(tuesday);
    }

    // Case 2
    @Test
    void rescheduleIntoAFullALevelOrAStartedSlotIsRefused() throws Exception {
        UUID saturday = slot(SAT_17, TEN, 6);
        UUID sara = booking(member("Sara Ali"), saturday, ahmed, "upcoming");
        UUID full = slot(TUE_20, TEN, 1);
        booking(member("Seat Taker"), full, ahmed, "upcoming");
        UUID levelSession = levelSlot(TUE_20, LocalTime.of(17, 0));
        UUID started = slot(FRI_16, TEN, 6);

        change(sara, "{ \"slotId\": \"%s\" }".formatted(full))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("slot_full"));
        change(sara, "{ \"slotId\": \"%s\" }".formatted(levelSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("slot_not_found"));
        change(sara, "{ \"slotId\": \"%s\" }".formatted(started))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("slot_closed"));
        change(sara, "{ \"slotId\": \"%s\" }".formatted(UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("slot_not_found"));

        assertThat(sessionOf(sara)).isEqualTo(saturday);
        assertThat(upcomingIn(full)).isEqualTo(1);
    }

    // Case 3
    @Test
    void rescheduleToTheCurrentSlotChangesNothing() throws Exception {
        // A full slot, so a capacity check would refuse it.
        UUID saturday = slot(SAT_17, TEN, 1);
        UUID sara = booking(member("Sara Ali"), saturday, ahmed, "upcoming");
        long version = versionOf(sara);

        change(sara, "{ \"slotId\": \"%s\" }".formatted(saturday))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sara.toString()))
                .andExpect(jsonPath("$.slot.id").value(saturday.toString()));

        assertThat(versionOf(sara)).isEqualTo(version);
    }

    // Case 4
    @Test
    void aWaitingBookingCannotBeMoved() throws Exception {
        UUID waleed = booking(member("Waleed Waiting"), slot(FRI_16, TEN, 6), ahmed, "upcoming");
        UUID open = slot(SAT_17, TEN, 6);

        change(waleed, "{ \"slotId\": \"%s\" }".formatted(open))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("slot_started"));
        assertThat(upcomingIn(open)).isZero();
    }

    // Case 5
    @Test
    void rebookANoShowCreatesANewBookingWithTheSameAssessor() throws Exception {
        UUID omar = member("Omar Hassan");
        UUID noShow = booking(omar, slot(LocalDate.of(2026, 10, 10), TEN, 6), ahmed, "noshow");
        UUID sat24 = slot(SAT_24, TEN, 6);

        String fresh = id(change(noShow, "{ \"slotId\": \"%s\" }".formatted(sat24))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("upcoming"))
                .andExpect(jsonPath("$.slot.id").value(sat24.toString()))
                .andExpect(jsonPath("$.member.id").value(omar.toString()))
                .andExpect(jsonPath("$.assessor.id").value(ahmed.toString())));

        assertThat(fresh).isNotEqualTo(noShow.toString());
        assertThat(statusOf(noShow)).isEqualTo("noshow");
        assertThat(sessionOf(noShow)).isNotEqualTo(sat24);
        assertThat(bookingsOf(omar)).isEqualTo(2);
    }

    // Case 6
    @Test
    void rebookACancelledBookingWithAnotherAssessor() throws Exception {
        UUID camelia = member("Camelia Cancelled");
        UUID cancelled = booking(camelia, slot(LocalDate.of(2026, 10, 18), TEN, 6), ahmed, "cancelled");
        UUID sat24 = slot(SAT_24, TEN, 6);

        String fresh = id(change(cancelled, "{ \"slotId\": \"%s\", \"assessorId\": \"%s\" }".formatted(sat24, mona))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("upcoming"))
                .andExpect(jsonPath("$.assessor.id").value(mona.toString()))
                .andExpect(jsonPath("$.assessor.name").value("Mona Kamal")));

        assertThat(fresh).isNotEqualTo(cancelled.toString());
        assertThat(assessorOf(cancelled)).isEqualTo(ahmed);
        assertThat(statusOf(cancelled)).isEqualTo("cancelled");
    }

    // Case 7
    @Test
    void rebookingTheSameNoShowTwiceIsSuperseded() throws Exception {
        UUID omar = member("Omar Hassan");
        UUID noShow = booking(omar, slot(LocalDate.of(2026, 10, 10), TEN, 6), ahmed, "noshow");
        UUID sat24 = slot(SAT_24, TEN, 6);
        UUID tue20 = slot(TUE_20, TEN, 6);

        change(noShow, "{ \"slotId\": \"%s\" }".formatted(sat24)).andExpect(status().isOk());
        change(noShow, "{ \"slotId\": \"%s\" }".formatted(tue20))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("booking_superseded"));
        // The assessor of a superseded row can't be changed either: it is history.
        change(noShow, "{ \"assessorId\": \"%s\" }".formatted(mona))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("booking_superseded"));

        assertThat(bookingsOf(omar)).isEqualTo(2);
        assertThat(upcomingIn(tue20)).isZero();
    }

    // Case 8
    @Test
    void rebookKeepingADisabledAssessorIsRefused() throws Exception {
        UUID hany = trainer("Hany Gone", "disabled");
        UUID omar = member("Omar Hassan");
        UUID noShow = booking(omar, slot(LocalDate.of(2026, 10, 10), TEN, 6), hany, "noshow");
        UUID sat24 = slot(SAT_24, TEN, 6);

        change(noShow, "{ \"slotId\": \"%s\" }".formatted(sat24))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("trainer_not_active"))
                .andExpect(jsonPath("$.detail").value("Hany Gone is not an active trainer. Choose another assessor."))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("assessorId"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("trainer_not_active"));

        assertThat(bookingsOf(omar)).isEqualTo(1);
        assertThat(upcomingIn(sat24)).isZero();
    }

    // Case 9
    @Test
    void theAssessorCanBeChangedWhileUpcomingWaitingNoShowOrCancelled() throws Exception {
        UUID upcoming = booking(member("Sara Ali"), slot(SAT_17, TEN, 6), ahmed, "upcoming");
        UUID waiting = booking(member("Waleed Waiting"), slot(FRI_16, TEN, 6), ahmed, "upcoming");
        UUID noShow = booking(member("Nader Noshow"), slot(LocalDate.of(2026, 10, 14), TEN, 6), ahmed, "noshow");
        UUID cancelled = booking(member("Camelia Cancelled"), slot(LocalDate.of(2026, 10, 18), TEN, 6), ahmed,
                "cancelled");

        for (var entry : List.of(List.of(upcoming, "upcoming"), List.of(waiting, "waiting"),
                List.of(noShow, "noshow"), List.of(cancelled, "cancelled"))) {
            UUID booking = (UUID) entry.get(0);
            change(booking, "{ \"assessorId\": \"%s\" }".formatted(mona))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(booking.toString()))
                    .andExpect(jsonPath("$.status").value(entry.get(1)))
                    .andExpect(jsonPath("$.assessor.id").value(mona.toString()))
                    .andExpect(jsonPath("$.assessor.name").value("Mona Kamal"));
            assertThat(assessorOf(booking)).isEqualTo(mona);
        }
    }

    // Case 10
    @Test
    void aPlacedBookingCannotBeChanged() throws Exception {
        UUID placed = booking(member("Pakinam Placed"), slot(LocalDate.of(2026, 10, 15), TEN, 6), ahmed, "placed");
        UUID open = slot(SAT_17, TEN, 6);

        change(placed, "{ \"assessorId\": \"%s\" }".formatted(mona))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("booking_closed"));
        change(placed, "{ \"slotId\": \"%s\" }".formatted(open))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("booking_closed"));
        change(placed, "{ \"slotId\": \"%s\", \"assessorId\": \"%s\" }".formatted(open, mona))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("booking_closed"));

        assertThat(assessorOf(placed)).isEqualTo(ahmed);
        assertThat(upcomingIn(open)).isZero();
    }

    // Case 11
    @Test
    void theNewAssessorMustBeAnExistingActiveTrainer() throws Exception {
        UUID sara = booking(member("Sara Ali"), slot(SAT_17, TEN, 6), ahmed, "upcoming");
        UUID invited = trainer("Ines Invited", "invited");
        UUID disabled = trainer("Hany Gone", "disabled");

        change(sara, "{ \"assessorId\": \"%s\" }".formatted(UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("trainer_not_found"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("assessorId"));
        change(sara, "{ \"assessorId\": \"%s\" }".formatted(invited))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("trainer_not_active"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("assessorId"));
        change(sara, "{ \"assessorId\": \"%s\" }".formatted(disabled))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("trainer_not_active"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("assessorId"));

        assertThat(assessorOf(sara)).isEqualTo(ahmed);
    }

    // Case 12
    @Test
    void anEmptyBodyHasNothingToChange() throws Exception {
        UUID sara = booking(member("Sara Ali"), slot(SAT_17, TEN, 6), ahmed, "upcoming");

        change(sara, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("slotId"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("nothing_to_change"));
    }

    // Case 13
    @Test
    void anUnknownBookingIsNotFound() throws Exception {
        change(UUID.randomUUID(), "{ \"assessorId\": \"%s\" }".formatted(mona))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("booking_not_found"));
    }

    // Case 14
    @Test
    void afterTheChangeOnlyTheNewAssessorRecordsTheResult() throws Exception {
        UUID waiting = booking(member("Waleed Waiting"), slot(FRI_16, TEN, 6), ahmed, "upcoming");
        change(waiting, "{ \"assessorId\": \"%s\" }".formatted(mona)).andExpect(status().isOk());

        recordAttended(waiting, ahmed)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("not_assessor"));
        recordAttended(waiting, mona)
                .andExpect(status().is2xxSuccessful());

        assertThat(statusOf(waiting)).isEqualTo("placed");
        assertThat(jdbc.queryForObject("select trainer_id from rider", UUID.class)).isEqualTo(mona);
    }

    // Case 15
    @Test
    void twoReschedulesIntoTheLastSeatOneWins() throws Exception {
        UUID saturday = slot(SAT_17, TEN, 6);
        UUID sara = booking(member("Sara Ali"), saturday, ahmed, "upcoming");
        UUID laila = booking(member("Laila Hassan"), saturday, ahmed, "upcoming");
        UUID lastSeat = slot(TUE_20, TEN, 2);
        booking(member("Seat Taker"), lastSeat, ahmed, "upcoming");

        List<String> outcomes = race(
                () -> change(sara, "{ \"slotId\": \"%s\" }".formatted(lastSeat)).andReturn(),
                () -> change(laila, "{ \"slotId\": \"%s\" }".formatted(lastSeat)).andReturn());

        assertThat(outcomes).containsExactlyInAnyOrder("ok", "slot_full");
        assertThat(upcomingIn(lastSeat)).isEqualTo(2);
        assertThat(upcomingIn(saturday)).isEqualTo(1);
    }

    // Case 16
    @Test
    void aRescheduleAndATrainerBookingIntoTheLastSeatOneWins() throws Exception {
        UUID sara = booking(member("Sara Ali"), slot(SAT_17, TEN, 6), ahmed, "upcoming");
        UUID lastSeat = slot(TUE_20, TEN, 2);
        booking(member("Seat Taker"), lastSeat, ahmed, "upcoming");

        // The admin's New person, through the service behind POST /admin/members.
        List<String> outcomes = raceOutcomes(
                () -> outcome(change(sara, "{ \"slotId\": \"%s\" }".formatted(lastSeat)).andReturn()),
                () -> {
                    try {
                        memberBooking.registerAndBook(new AdminNewMemberRequest(
                                "Walk In", "01077778888", "walk.in@mail.eg", lastSeat, mona));
                        return "ok";
                    } catch (ApiException e) {
                        return e.code();
                    }
                });

        assertThat(outcomes).containsExactlyInAnyOrder("ok", "slot_full");
        assertThat(upcomingIn(lastSeat)).isEqualTo(2);
    }

    // Case 17
    @Test
    void recordingAndChangingTheAssessorAtOnceAreSerialisedOnTheBooking() throws Exception {
        UUID waiting = booking(member("Waleed Waiting"), slot(FRI_16, TEN, 6), ahmed, "upcoming");

        List<String> outcomes = race(
                () -> recordAttended(waiting, ahmed).andReturn(),
                () -> change(waiting, "{ \"assessorId\": \"%s\" }".formatted(mona)).andReturn());

        if (outcomes.get(0).equals("ok")) {
            assertThat(outcomes.get(1)).isEqualTo("booking_closed");
            assertThat(statusOf(waiting)).isEqualTo("placed");
            assertThat(assessorOf(waiting)).isEqualTo(ahmed);
            assertThat(jdbc.queryForObject("select trainer_id from rider", UUID.class)).isEqualTo(ahmed);
        } else {
            assertThat(outcomes).containsExactly("not_assessor", "ok");
            assertThat(statusOf(waiting)).isEqualTo("upcoming");
            assertThat(assessorOf(waiting)).isEqualTo(mona);
            assertThat(jdbc.queryForObject("select count(*) from rider", Long.class)).isZero();
        }
    }

    // Case 18: the service accepts exactly what BookingActions offers on W2. A slotId is Reschedule or Rebook,
    // an assessorId is Change assessor; Remind and View rider are not PATCH actions.
    @ParameterizedTest
    @ValueSource(strings = {"upcoming", "waiting", "noshow", "cancelled", "placed"})
    void theChecksAgreeWithBookingActions(String shown) throws Exception {
        Set<BookingAction> allowed = BookingActions.allowed(shown, true);
        UUID open = slot(SAT_24, TEN, 6);

        UUID forSlot = bookingShown(shown, "Slot Change");
        MvcResult slotChange = change(forSlot, "{ \"slotId\": \"%s\" }".formatted(open)).andReturn();
        boolean moved = slotChange.getResponse().getStatus() == 200;
        if (allowed.contains(BookingAction.RESCHEDULE)) {
            assertThat(moved).as("reschedule %s", shown).isTrue();
            assertThat(JsonPath.<String>read(body(slotChange), "$.id")).isEqualTo(forSlot.toString());
        } else if (allowed.contains(BookingAction.REBOOK)) {
            assertThat(moved).as("rebook %s", shown).isTrue();
            assertThat(JsonPath.<String>read(body(slotChange), "$.id")).isNotEqualTo(forSlot.toString());
        } else {
            assertThat(moved).as("slot change on %s", shown).isFalse();
        }

        UUID forAssessor = bookingShown(shown, "Assessor Change");
        int assessorStatus = change(forAssessor, "{ \"assessorId\": \"%s\" }".formatted(mona)).andReturn()
                .getResponse().getStatus();
        assertThat(assessorStatus == 200).as("change assessor on %s", shown)
                .isEqualTo(allowed.contains(BookingAction.CHANGE_ASSESSOR));
    }

    // Case 19
    @Test
    void onlyTheAdminGetsIn() throws Exception {
        UUID sara = booking(member("Sara Ali"), slot(SAT_17, TEN, 6), ahmed, "upcoming");
        String body = "{ \"assessorId\": \"%s\" }".formatted(mona);

        mvc.perform(patch(PATH + sara).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(asTrainer(patch(PATH + sara), ahmed).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        assertThat(assessorOf(sara)).isEqualTo(ahmed);
    }

    // A booking W2 shows with that status, assessed by Ahmed, for a new member.
    private UUID bookingShown(String shown, String name) {
        UUID member = member(name);
        return switch (shown) {
            case "upcoming" -> booking(member, slot(SAT_17, TEN, 6), ahmed, "upcoming");
            case "waiting" -> booking(member, slot(FRI_16, TEN, 6), ahmed, "upcoming");
            case "noshow", "placed" -> booking(member, slot(LocalDate.of(2026, 10, 14), TEN, 6), ahmed, shown);
            case "cancelled" -> booking(member, slot(LocalDate.of(2026, 10, 18), TEN, 6), ahmed, shown);
            default -> throw new IllegalArgumentException(shown);
        };
    }

    private ResultActions change(UUID bookingId, String body) throws Exception {
        return mvc.perform(asAdmin(patch(PATH + bookingId)).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions recordAttended(UUID bookingId, UUID trainer) throws Exception {
        return mvc.perform(asTrainer(post("/me/bookings/" + bookingId + "/result"), trainer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"attended\": true, \"levelId\": 2 }"));
    }

    // Starts both calls at once; each result is "ok" or the code the API answered with.
    private static List<String> race(Callable<MvcResult> first, Callable<MvcResult> second) throws Exception {
        return raceOutcomes(() -> outcome(first.call()), () -> outcome(second.call()));
    }

    // Like race, for calls that report their own outcome ("ok" or a code).
    private static List<String> raceOutcomes(Callable<String> first, Callable<String> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> a = pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return first.call();
            });
            Future<String> b = pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return second.call();
            });
            start.countDown();
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    private static String outcome(MvcResult result) throws Exception {
        int status = result.getResponse().getStatus();
        assertThat(status).as("never a 500").isLessThan(500);
        return status < 300 ? "ok" : JsonPath.read(body(result), "$.code");
    }

    private static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(body(result.andReturn()), "$.id");
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(UUID.randomUUID(), Role.ADMIN)));
    }

    private MockHttpServletRequestBuilder asTrainer(MockHttpServletRequestBuilder request, UUID trainer) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(trainer, Role.TRAINER)));
    }

    private String bearer(AuthUser user) {
        return "Bearer " + jwtService.issue(user).value();
    }

    private static Instant cairo(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, CAIRO).toInstant();
    }

    private long upcomingIn(UUID slot) {
        return jdbc.queryForObject(
                "select count(*) from assessment_booking where session_id = ? and status = 'upcoming'",
                Long.class, slot);
    }

    private long bookingsOf(UUID member) {
        return jdbc.queryForObject("select count(*) from assessment_booking where member_id = ?", Long.class, member);
    }

    private UUID sessionOf(UUID booking) {
        return jdbc.queryForObject("select session_id from assessment_booking where id = ?", UUID.class, booking);
    }

    private UUID assessorOf(UUID booking) {
        return jdbc.queryForObject("select assessor_id from assessment_booking where id = ?", UUID.class, booking);
    }

    private String statusOf(UUID booking) {
        return jdbc.queryForObject("select status from assessment_booking where id = ?", String.class, booking);
    }

    private long versionOf(UUID booking) {
        return jdbc.queryForObject("select version from assessment_booking where id = ?", Long.class, booking);
    }

    private UUID trainer(String name, String status) {
        seq++;
        return jdbc.queryForObject("""
                        insert into trainer (name, phone, email, status, password_hash)
                        values (?, ?, ?, ?, ?) returning id""",
                UUID.class, name, "0199999%04d".formatted(seq), "trainer%d@school.eg".formatted(seq), status,
                status.equals("invited") ? null : "not-a-real-hash");
    }

    private UUID member(String name) {
        seq++;
        return jdbc.queryForObject("insert into member (name, phone, email) values (?, ?, ?) returning id",
                UUID.class, name, "0128888%04d".formatted(seq), "member%d@mail.eg".formatted(seq));
    }

    // An assessment slot (no level) on that date, one hour long. Saved through JPA, as the app saves them,
    // so the start and end times are stored the way the app reads them.
    private UUID slot(LocalDate date, LocalTime start, int capacity) {
        return session(date, start, capacity, false);
    }

    // A Level 2 session: not an assessment slot.
    private UUID levelSlot(LocalDate date, LocalTime start) {
        return session(date, start, 6, true);
    }

    private UUID session(LocalDate date, LocalTime start, int capacity, boolean level) {
        return new TransactionTemplate(transactionManager).execute(s -> {
            SessionTemplate template = new SessionTemplate();
            template.setDayOfWeek(date.getDayOfWeek());
            template.setStartTime(start);
            template.setEndTime(start.plusHours(1));
            template.setCapacity(capacity);
            template.setValidFrom(date);
            if (level) {
                template.setLevel(levelRepository.getReferenceById((short) 2));
            }
            template = sessionTemplateRepository.save(template);
            return sessionRepository.save(Session.of(template, date)).getId();
        });
    }

    // The result columns follow the status as the table's check requires; placed members go to level 2.
    // Each booking is a second newer than the last, so "latest per member" is unambiguous.
    private UUID booking(UUID member, UUID slot, UUID assessor, String status) {
        Boolean attended = switch (status) {
            case "placed" -> true;
            case "noshow" -> false;
            default -> null;
        };
        boolean recorded = attended != null;
        Timestamp cancelledAt = status.equals("cancelled") ? Timestamp.from(FRIDAY_NOON) : null;
        Timestamp created = Timestamp.from(FRIDAY_NOON.minusSeconds(86_400L * 7).plusSeconds(seq++));
        return jdbc.queryForObject("""
                        insert into assessment_booking
                            (member_id, session_id, assessor_id, status, attended, level_id, recorded_at,
                             cancelled_at, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?) returning id""",
                UUID.class, member, slot, assessor, status, attended, status.equals("placed") ? 2 : null,
                recorded ? Timestamp.from(FRIDAY_NOON.minusSeconds(3600)) : null, cancelledAt, created);
    }
}
