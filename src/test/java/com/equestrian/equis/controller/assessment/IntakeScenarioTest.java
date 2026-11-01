package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.support.TestPostgres;
import com.equestrian.equis.support.AdminOversightFixture;
import com.equestrian.equis.support.AdminOversightFixture.Coach;
import com.equestrian.equis.support.MutableClock;
import com.equestrian.equis.support.TrainerAccountsFixture;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static com.equestrian.equis.support.AdminOversightFixture.FIVE_PM;
import static com.equestrian.equis.support.AdminOversightFixture.SAT_17;
import static com.equestrian.equis.support.AdminOversightFixture.SAT_24;
import static com.equestrian.equis.support.AdminOversightFixture.TEN;
import static com.equestrian.equis.support.AdminOversightFixture.TUE_20;
import static com.equestrian.equis.support.AdminOversightFixture.race;
import static com.equestrian.equis.support.AdminOversightFixture.read;
import static com.equestrian.equis.support.AdminOversightFixture.readValue;
import static com.equestrian.equis.support.TrainerAccountsFixture.ADMIN_EMAIL;
import static com.equestrian.equis.support.TrainerAccountsFixture.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E11 intake journeys 1–14 end to end, over HTTP only: the admin on W2 and the trainers in the app (A3, A5), with
// the clock moved across slot starts. After every step assertInvariants() checks the rules that span endpoints
// (scenario 15). The single rules are in each endpoint's own *ApiTest.
// Every scenario starts on Friday 16 Oct 2026 10:00 with trainers Ahmed and Mona and the fixture's schedule:
// Saturday 10:00 assessments for 2, Tuesday 17:00 assessments for 4.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Import(AdminOversightFixture.Config.class)
class IntakeScenarioTest {
    private static final int LEVEL_ONE = 1;

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
    AdminOversightFixture fx;

    Coach ahmed;
    Coach mona;
    UUID sat17;
    UUID tue20;
    UUID sat24;
    int emails;

    // A person as W2 knows them: who they are and the booking their row shows.
    record Person(UUID memberId, UUID bookingId) {
    }

    @BeforeEach
    void setUp() throws Exception {
        fx.reset();
        emails = 0;
        ahmed = fx.coach("Ahmed Said");
        mona = fx.coach("Mona Kamal");
        fx.schedule();
        sat17 = fx.slotOn(SAT_17, TEN);
        tue20 = fx.slotOn(TUE_20, FIVE_PM);
        sat24 = fx.slotOn(SAT_24, TEN);
    }

    // The races commit for real, so nothing may be left for the next class sharing this context.
    @AfterEach
    void cleanUp() {
        fx.reset();
    }

    // 1
    @Test
    void addedThenAttendedBecomesARider() throws Exception {
        Person omar = add("Omar Hassan", sat17, ahmed);
        fx.w2()
                .andExpect(jsonPath(at(omar) + ".status").value(contains("upcoming")))
                .andExpect(jsonPath(at(omar) + ".memberStatus").value(contains("pending_assessment")));

        fx.at(SAT_17, LocalTime.of(10, 5));
        assertInvariants();
        fx.w2()
                .andExpect(jsonPath(at(omar) + ".status").value(contains("waiting")))
                .andExpect(jsonPath(at(omar) + ".memberStatus").value(contains("pending_assessment")));

        String riderId = read(fx.recordResult(ahmed, omar.bookingId(), true, LEVEL_ONE)
                .andExpect(status().isOk()), "$.riderId");
        assertInvariants();
        fx.w2()
                .andExpect(jsonPath(at(omar) + ".status").value(contains("placed")))
                .andExpect(jsonPath(at(omar) + ".memberStatus").value(contains("rider")))
                .andExpect(jsonPath(at(omar) + ".riderId").value(contains(riderId)))
                .andExpect(jsonPath(at(omar) + ".actions[*]").value(contains("view_rider")));
    }

    // 2
    @Test
    void aNoShowIsRebookedAndThenAttends() throws Exception {
        Person omar = add("Omar Hassan", sat17, ahmed);
        fx.at(SAT_17, LocalTime.of(10, 30));
        fx.recordResult(ahmed, omar.bookingId(), false, null).andExpect(status().isOk());
        assertInvariants();
        fx.w2()
                .andExpect(jsonPath(at(omar) + ".status").value(contains("noshow")))
                .andExpect(jsonPath(at(omar) + ".memberStatus").value(contains("no_show")))
                .andExpect(jsonPath("$.totals.noshow").value(1));

        UUID second = UUID.fromString(read(rebook(omar.bookingId(), sat24).andExpect(status().isOk()), "$.id"));
        assertInvariants();
        assertThat(second).isNotEqualTo(omar.bookingId());
        assertThat(bookingsOf(omar)).isEqualTo(2);
        fx.w2()
                .andExpect(jsonPath(at(omar) + ".id").value(contains(second.toString())))
                .andExpect(jsonPath(at(omar) + ".status").value(contains("upcoming")))
                .andExpect(jsonPath("$.totals.noshow").value(0))
                .andExpect(jsonPath("$.totals.upcoming").value(1));

        fx.at(SAT_24, LocalTime.of(10, 30));
        fx.recordResult(ahmed, second, true, LEVEL_ONE).andExpect(status().isOk());
        assertInvariants();
        fx.w2()
                .andExpect(jsonPath(at(omar) + ".status").value(contains("placed")))
                .andExpect(jsonPath("$.totals.placed").value(1))
                .andExpect(jsonPath("$.totals.noshow").value(0));
        assertThat(bookingsOf(omar)).isEqualTo(2);
    }

    // 3
    @Test
    void aCancelBeforeTheSlotFreesTheSeatAndKeepsTheReason() throws Exception {
        Person omar = add("Omar Hassan", sat17, ahmed);

        cancel(omar.bookingId(), "person_unwell", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cancelled"))
                .andExpect(jsonPath("$.cancelReason").value("person_unwell"))
                .andExpect(jsonPath("$.memberStatus").value("pending_assessment"));
        assertInvariants();
        assertThat(fx.upcomingIn(sat17)).isZero();

        // W2 takes the newest booking by created_at; a frozen clock would give both bookings the same one.
        fx.accounts().advance(Duration.ofMinutes(1));
        UUID second = UUID.fromString(read(rebook(omar.bookingId(), sat24)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("upcoming"))
                .andExpect(jsonPath("$.memberStatus").value("pending_assessment")), "$.id"));
        assertInvariants();
        assertThat(fx.bookingStatus(omar.bookingId())).isEqualTo("cancelled");
        assertThat(fx.jdbc().queryForObject("select cancel_reason from assessment_booking where id = ?",
                String.class, omar.bookingId())).isEqualTo("person_unwell");
        assertThat(fx.upcomingIn(sat24)).isEqualTo(1);
        fx.w2().andExpect(jsonPath(at(omar) + ".id").value(contains(second.toString())));
    }

    // 4
    @Test
    void aClosedNoShowLeavesTheTotalsUntilReopenedAndRebooked() throws Exception {
        Person omar = add("Omar Hassan", sat17, ahmed);
        Person sara = add("Sara Ali", tue20, mona);
        fx.at(SAT_17, LocalTime.of(10, 30));
        fx.recordResult(ahmed, omar.bookingId(), false, null).andExpect(status().isOk());
        assertInvariants();
        fx.w2().andExpect(jsonPath("$.totals.noshow").value(1)).andExpect(jsonPath("$.totals.closed").value(0));

        close(omar).andExpect(status().isOk()).andExpect(jsonPath("$.memberStatus").value("closed"));
        assertInvariants();
        fx.w2()
                .andExpect(jsonPath("$.totals.noshow").value(0))
                .andExpect(jsonPath("$.totals.upcoming").value(1))
                .andExpect(jsonPath("$.totals.closed").value(1))
                .andExpect(jsonPath("$.bookings[*].member.id", contains(sara.memberId().toString())));
        fx.w2("noshow", null).andExpect(jsonPath("$.bookings.length()").value(0));
        fx.w2("closed", null).andExpect(jsonPath("$.bookings[*].member.id", contains(omar.memberId().toString())));

        reopen(omar)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("noshow"))
                .andExpect(jsonPath("$.memberStatus").value("no_show"))
                .andExpect(jsonPath("$.actions", hasItem("rebook")));
        assertInvariants();
        fx.w2().andExpect(jsonPath("$.totals.noshow").value(1)).andExpect(jsonPath("$.totals.closed").value(0));

        rebook(omar.bookingId(), sat24).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("upcoming"));
        assertInvariants();
        fx.w2().andExpect(jsonPath("$.totals.upcoming").value(2)).andExpect(jsonPath("$.totals.noshow").value(0));
    }

    // 5
    @Test
    void aClosedPersonsPhoneIsRefusedWithTheirRecordSoTheyCanBeReopened() throws Exception {
        String phone = fx.nextPhone();
        Person omar = person(addPerson("Omar Hassan", phone, nextEmail(), sat17, ahmed)
                .andExpect(status().isCreated()));
        fx.at(SAT_17, LocalTime.of(10, 30));
        fx.recordResult(ahmed, omar.bookingId(), false, null).andExpect(status().isOk());
        close(omar).andExpect(status().isOk());
        assertInvariants();

        String existing = read(addPerson("Omar H. Mahmoud", phone, nextEmail(), sat24, ahmed)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("phone_taken"))
                .andExpect(jsonPath("$.existingClosed").value(true))
                .andExpect(jsonPath("$.existingMemberId").value(omar.memberId().toString())), "$.existingMemberId");
        assertInvariants();
        assertThat(members()).isEqualTo(1);

        fx.accounts().admin(post("/admin/members/" + existing + "/reopen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.id").value(omar.memberId().toString()))
                .andExpect(jsonPath("$.memberStatus").value("no_show"));
        assertInvariants();
    }

    // 6
    @Test
    void aTrainersOrTheAdminsEmailIsRefusedWithoutNamingAnyone() throws Exception {
        String trainerEmail = fx.jdbc().queryForObject("select email from trainer where id = ?", String.class,
                ahmed.id());

        addPerson("Omar Hassan", fx.nextPhone(), trainerEmail, sat17, ahmed)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_in_use"))
                .andExpect(jsonPath("$.existingMemberId").doesNotExist());
        assertInvariants();
        addPerson("Omar Hassan", fx.nextPhone(), ADMIN_EMAIL, sat17, ahmed)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_in_use"))
                .andExpect(jsonPath("$.existingMemberId").doesNotExist());
        assertInvariants();
        assertThat(members()).isZero();
    }

    // 7
    @Test
    void aPersonWithAnUpcomingAssessmentIsClosedOnlyAfterTheCancel() throws Exception {
        Person omar = add("Omar Hassan", sat17, ahmed);

        close(omar).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("member_has_active_booking"));
        assertInvariants();
        cancel(omar.bookingId(), "person_cancelled", null).andExpect(status().isOk());
        assertInvariants();
        close(omar).andExpect(status().isOk()).andExpect(jsonPath("$.memberStatus").value("closed"));
        assertInvariants();
    }

    // 8
    @Test
    void afterAnAssessorChangeOnlyTheNewAssessorRecords() throws Exception {
        Person omar = add("Omar Hassan", sat17, ahmed);
        fx.at(SAT_17, LocalTime.of(10, 30));

        changeAssessor(omar.bookingId(), mona.id())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("waiting"))
                .andExpect(jsonPath("$.assessor.id").value(mona.id().toString()));
        assertInvariants();

        fx.recordResult(ahmed, omar.bookingId(), true, LEVEL_ONE)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("not_assessor"));
        assertInvariants();
        fx.recordResult(mona, omar.bookingId(), true, LEVEL_ONE).andExpect(status().isOk());
        assertInvariants();
        fx.recordResult(ahmed, omar.bookingId(), true, LEVEL_ONE)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("not_assessor"));
        fx.w2().andExpect(jsonPath(at(omar) + ".status").value(contains("placed")));
    }

    // 9
    @Test
    void anEditedPersonIsFoundByTheNewEmailAndTheOldPhoneIsFree() throws Exception {
        String oldPhone = fx.nextPhone();
        Person omar = person(addPerson("Omar Hassan", oldPhone, nextEmail(), sat17, ahmed)
                .andExpect(status().isCreated()));

        String newPhone = fx.nextPhone();
        editPerson(omar, "{ \"phone\": \"%s\", \"email\": \"Omar.New@Mail.eg\" }".formatted(newPhone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.phone").value(newPhone))
                .andExpect(jsonPath("$.member.email").value("omar.new@mail.eg"));
        assertInvariants();

        fx.w2(null, "omar.new@mail.eg")
                .andExpect(jsonPath("$.bookings[*].member.id", contains(omar.memberId().toString())));
        addPerson("Sara Ali", oldPhone, nextEmail(), sat17, mona).andExpect(status().isCreated());
        assertInvariants();
        assertThat(members()).isEqualTo(2);
    }

    // 10
    @Test
    void theTrainerCanNoLongerAddAPersonButSeesTheAdminsBooking() throws Exception {
        String body = """
                { "name": "Walk In", "phone": "%s", "email": "%s", "slotId": "%s" }"""
                .formatted(fx.nextPhone(), nextEmail(), sat17);
        fx.asCoach(ahmed, post("/me/members").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));
        assertInvariants();
        assertThat(members()).isZero();

        Person omar = add("Omar Hassan", sat17, ahmed);
        fx.slotBookings(ahmed, sat17)
                .andExpect(jsonPath("$.bookings[*].memberId", contains(omar.memberId().toString())))
                .andExpect(jsonPath("$.bookings[0].mine").value(true))
                .andExpect(jsonPath("$.bookings[0].status").value("upcoming"));
        fx.slotBookings(mona, sat17)
                .andExpect(jsonPath("$.bookings[*].memberId", contains(omar.memberId().toString())))
                .andExpect(jsonPath("$.bookings[0].mine").value(false));
    }

    // 11
    @Test
    void anAddAndARescheduleRaceForTheLastSeat() throws Exception {
        add("First In", sat17, ahmed);
        Person sara = add("Sara Ali", tue20, mona);
        fx.w2();          // signs the admin in before the two threads share the token
        String phone = fx.nextPhone();
        String email = nextEmail();

        List<String> outcomes = race(
                () -> addPerson("Last Seat", phone, email, sat17, ahmed).andReturn(),
                () -> rebook(sara.bookingId(), sat17).andReturn());

        assertThat(outcomes).containsExactlyInAnyOrder("ok", "slot_full");
        assertThat(fx.upcomingIn(sat17)).isEqualTo(2);
        assertInvariants();
    }

    // 12
    @Test
    void aCloseAndARebookOfTheSameNoShowRace() throws Exception {
        Person omar = add("Omar Hassan", sat17, ahmed);
        fx.at(SAT_17, LocalTime.of(10, 30));
        fx.recordResult(ahmed, omar.bookingId(), false, null).andExpect(status().isOk());
        fx.w2();

        List<String> outcomes = race(
                () -> close(omar).andReturn(),
                () -> rebook(omar.bookingId(), sat24).andReturn());

        assertThat(outcomes).filteredOn("ok"::equals).hasSize(1);
        assertThat(outcomes).filteredOn(o -> !"ok".equals(o))
                .singleElement().isIn("member_closed", "member_has_active_booking");
        boolean closed = fx.jdbc().queryForObject("select closed_at is not null from member where id = ?",
                Boolean.class, omar.memberId());
        assertThat(fx.upcomingIn(sat24)).isEqualTo(closed ? 0 : 1);
        assertThat(bookingsOf(omar)).isEqualTo(closed ? 1 : 2);
        assertInvariants();
    }

    // 13
    @Test
    void aRiderCanBeNeitherClosedNorEditedHere() throws Exception {
        Person omar = add("Omar Hassan", sat17, ahmed);
        fx.at(SAT_17, LocalTime.of(10, 30));
        fx.recordResult(ahmed, omar.bookingId(), true, LEVEL_ONE).andExpect(status().isOk());
        assertInvariants();

        close(omar).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("member_is_rider"));
        assertInvariants();
        editPerson(omar, "{ \"name\": \"Omar H.\" }")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("member_is_rider"));
        assertInvariants();
        fx.w2().andExpect(jsonPath(at(omar) + ".member.name").value(contains("Omar Hassan")));
    }

    // 14
    @Test
    void onlyAnUpcomingAssessmentIsCancelledAndOnlyOnce() throws Exception {
        Person omar = add("Omar Hassan", sat17, ahmed);
        Person sara = add("Sara Ali", sat24, mona);
        fx.at(SAT_17, LocalTime.of(10, 5));

        cancel(omar.bookingId(), "weather", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("slot_started"));
        assertInvariants();
        cancel(sara.bookingId(), "other", "Moved abroad").andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelReasonText").value("Moved abroad"));
        assertInvariants();
        cancel(sara.bookingId(), "person_cancelled", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("booking_not_upcoming"));
        assertInvariants();
        assertThat(fx.bookingStatus(omar.bookingId())).isEqualTo("upcoming");
    }

    // ---- 15: the rules that span the endpoints

    // The six W2 totals add up to the number of people; nobody holds more than one upcoming booking; no slot holds
    // more upcoming bookings than its capacity; no closed person holds an upcoming booking.
    // "upcoming" is the stored status, so it also covers bookings W2 shows as waiting.
    private void assertInvariants() throws Exception {
        ResultActions w2 = fx.w2();
        long totals = 0;
        for (String name : List.of("upcoming", "waiting", "placed", "noshow", "cancelled", "closed")) {
            totals += readValue(w2, "$.totals." + name) instanceof Number n ? n.longValue() : 0;
        }
        assertThat(totals).as("W2 totals add up to the number of people").isEqualTo(members());
        assertThat(count("""
                select count(*) from (
                    select member_id from assessment_booking where status = 'upcoming'
                    group by member_id having count(*) > 1) x"""))
                .as("people with more than one upcoming booking").isZero();
        assertThat(count("""
                select count(*) from session s
                where (select count(*) from assessment_booking b
                       where b.session_id = s.id and b.status = 'upcoming') > s.capacity"""))
                .as("slots over capacity").isZero();
        assertThat(count("""
                select count(*) from assessment_booking b join member m on m.id = b.member_id
                where b.status = 'upcoming' and m.closed_at is not null"""))
                .as("closed people with an upcoming booking").isZero();
    }

    // ---- The admin on W2

    // New person with an email no one else in the scenario has.
    private Person add(String name, UUID slotId, Coach assessor) throws Exception {
        Person p = person(addPerson(name, fx.nextPhone(), nextEmail(), slotId, assessor)
                .andExpect(status().isCreated()));
        assertInvariants();
        return p;
    }

    private ResultActions addPerson(String name, String phone, String email, UUID slotId, Coach assessor)
            throws Exception {
        return fx.accounts().admin(post("/admin/members").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "%s", "phone": "%s", "email": "%s", "slotId": "%s", "assessorId": "%s" }"""
                        .formatted(name, phone, email, slotId, assessor.id())));
    }

    private ResultActions cancel(UUID bookingId, String reason, String text) throws Exception {
        String body = text == null
                ? "{ \"reason\": \"%s\" }".formatted(reason)
                : "{ \"reason\": \"%s\", \"reasonText\": \"%s\" }".formatted(reason, text);
        return fx.accounts().admin(post("/admin/bookings/" + bookingId + "/cancel")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions close(Person p) throws Exception {
        return fx.accounts().admin(post("/admin/members/" + p.memberId() + "/close"));
    }

    private ResultActions reopen(Person p) throws Exception {
        return fx.accounts().admin(post("/admin/members/" + p.memberId() + "/reopen"));
    }

    private ResultActions rebook(UUID bookingId, UUID slotId) throws Exception {
        return fx.patchBooking(bookingId, "{ \"slotId\": \"%s\" }".formatted(slotId));
    }

    private ResultActions changeAssessor(UUID bookingId, UUID trainerId) throws Exception {
        return fx.patchBooking(bookingId, "{ \"assessorId\": \"%s\" }".formatted(trainerId));
    }

    private ResultActions editPerson(Person p, String changes) throws Exception {
        return fx.accounts().admin(patch("/admin/members/" + p.memberId())
                .contentType(MediaType.APPLICATION_JSON).content(changes));
    }

    // The person's row in a W2 answer; JsonPath filters answer with a list.
    private static String at(Person p) {
        return "$.bookings[?(@.member.id == '" + p.memberId() + "')]";
    }

    // ---- Helpers

    private static Person person(ResultActions created) throws Exception {
        String body = created.andReturn().getResponse().getContentAsString();
        return new Person(UUID.fromString(JsonPath.read(body, "$.member.id")),
                UUID.fromString(JsonPath.read(body, "$.id")));
    }

    private String nextEmail() {
        emails++;
        return "intake%d@mail.eg".formatted(emails);
    }

    private long members() {
        return count("select count(*) from member");
    }

    private long bookingsOf(Person p) {
        return fx.jdbc().queryForObject("select count(*) from assessment_booking where member_id = ?", Long.class,
                p.memberId());
    }

    private long count(String sql) {
        return fx.jdbc().queryForObject(sql, Long.class);
    }
}
