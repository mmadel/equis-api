package com.equestrian.equis.service.assessment;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.assessment.AssessmentSlotDayDto;
import com.equestrian.equis.dto.assessment.AssessmentSlotDto;
import com.equestrian.equis.dto.assessment.AssessmentSlotsDto;
import com.equestrian.equis.dto.assessment.BookingRowDto;
import com.equestrian.equis.dto.assessment.SlotDetailDto;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.Member;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.assessment.MemberRepository;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import com.equestrian.equis.support.TestMembers;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AssessmentSlotQueryService.class, AssessmentSlotMapper.class, AssessmentRules.class})
public class AssessmentSlotQueryServiceTest {
    // The clock is fixed on this Wednesday at 09:00 school time (Africa/Cairo).
    private static final Instant NOW = Instant.parse("2026-10-15T06:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final LocalTime NINE = LocalTime.of(9, 0);
    private static final LocalTime TEN = LocalTime.of(10, 0);

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @Autowired
    AssessmentSlotQueryService service;
    @Autowired
    AssessmentBookingRepository bookingRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    LevelRepository levelRepository;
    @Autowired
    EntityManager em;

    Trainer ahmed;
    Trainer mona;
    // Each booking is made a second after the last: on Windows two saves can share the clock's instant.
    int bookingsMade;

    @BeforeEach
    void setUp() {
        ahmed = trainer("Ahmed Fathy", "01999990021", "slot-query-test-ahmed@example.com");
        mona = trainer("Mona Adel", "01999990022", "slot-query-test-mona@example.com");
    }

    // ---- list ----

    @Test
    void withoutDatesTheNextTwoWeeksAreListed() {
        Session first = slot(TODAY, TEN, 4);
        Session last = slot(TODAY.plusDays(13), TEN, 4);
        slot(TODAY.minusDays(1), TEN, 4);
        slot(TODAY.plusDays(14), TEN, 4);

        AssessmentSlotsDto result = service.slots(null, null);

        assertThat(result.from()).isEqualTo(TODAY);
        assertThat(result.to()).isEqualTo(LocalDate.of(2026, 10, 28));
        assertThat(slotIds(result)).containsExactly(first.getId(), last.getId());
    }

    @Test
    void slotsAreGroupedByDateInStartOrderWithoutLevelOrCancelledSessions() {
        LocalDate saturday = LocalDate.of(2026, 10, 17);
        LocalDate tuesday = LocalDate.of(2026, 10, 20);
        Session late = slot(saturday, LocalTime.of(12, 0), 4);
        Session early = slot(saturday, TEN, 4);
        Session other = slot(tuesday, LocalTime.of(17, 0), 2);
        levelSession(saturday);
        Session cancelled = slot(tuesday.plusDays(1), TEN, 4);
        cancelled.cancel(NOW);
        sessionRepository.saveAndFlush(cancelled);

        AssessmentSlotsDto result = service.slots(TODAY, TODAY.plusDays(13));

        assertThat(result.days()).extracting(AssessmentSlotDayDto::date).containsExactly(saturday, tuesday);
        assertThat(result.days().get(0).slots()).containsExactly(
                new AssessmentSlotDto(early.getId(), saturday, TEN, LocalTime.of(11, 0), 4, 0, true),
                new AssessmentSlotDto(late.getId(), saturday, LocalTime.of(12, 0), LocalTime.of(13, 0), 4, 0, true));
        assertThat(result.days().get(1).slots()).extracting(AssessmentSlotDto::id).containsExactly(other.getId());
    }

    @Test
    void bookedLeavesOutCancelledBookingsAndAFullSlotIsNotOpen() {
        Session roomy = slot(TODAY.plusDays(3), TEN, 4);
        book(roomy, ahmed);
        book(roomy, mona);
        cancelledBooking(roomy);
        Session full = slot(TODAY.plusDays(6), TEN, 2);
        book(full, ahmed);
        book(full, ahmed);

        List<AssessmentSlotDto> slots = flat(service.slots(null, null));

        assertThat(slots).extracting(AssessmentSlotDto::booked, AssessmentSlotDto::open)
                .containsExactly(tuple(2L, true),
                        tuple(2L, false));
    }

    @Test
    void aSlotStopsBeingOpenAtItsStartTimeButIsStillListed() {
        Session started = slot(TODAY, NINE, 4);
        Session nextMinute = slot(TODAY, NINE.plusMinutes(1), 4);

        assertThat(flat(service.slots(null, null))).containsExactly(
                new AssessmentSlotDto(started.getId(), TODAY, NINE, LocalTime.of(10, 0), 4, 0, false),
                new AssessmentSlotDto(nextMinute.getId(), TODAY, NINE.plusMinutes(1), LocalTime.of(10, 1), 4, 0, true));
    }

    @Test
    void aPastSlotKeepsCountingItsResults() {
        Session past = slot(TODAY.minusWeeks(1), TEN, 4);
        placed(book(past, ahmed), "Good seat");
        noShow(book(past, ahmed));

        List<AssessmentSlotDto> slots = flat(service.slots(TODAY.minusWeeks(1), TODAY));

        assertThat(slots).extracting(AssessmentSlotDto::id, AssessmentSlotDto::booked, AssessmentSlotDto::open)
                .containsExactly(tuple(past.getId(), 2L, false));
    }

    @Test
    void aRangeWithoutSlotsHasNoDays() {
        levelSession(TODAY.plusDays(1));

        assertThat(service.slots(null, null).days()).isEmpty();
    }

    @Test
    void rangeMustBeInOrderAndAtMost56Days() {
        assertRefused(() -> service.slots(TODAY, TODAY.minusDays(1)), HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_RANGE);
        assertRefused(() -> service.slots(TODAY, TODAY.plusDays(56)), HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_RANGE);
        // Only to given: the default from is today, which is after it.
        assertRefused(() -> service.slots(null, TODAY.minusDays(1)), HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_RANGE);

        assertThat(service.slots(TODAY, TODAY.plusDays(55)).days()).isEmpty();
    }

    // ---- one slot ----

    @Test
    void aSlotBeforeItStartsListsItsBookingsInBookingOrder() {
        Session slot = slot(TODAY.plusDays(3), TEN, 4);
        AssessmentBooking sara = book(slot, ahmed, "Sara Mahmoud", "01012345678");
        AssessmentBooking omar = book(slot, ahmed, "Omar Said", null);
        AssessmentBooking laila = book(slot, ahmed, "Laila Hassan", "01012345679");

        SlotDetailDto detail = service.slot(slot.getId(), ahmed.getId());

        assertThat(detail).usingRecursiveComparison().ignoringFields("bookings").isEqualTo(new SlotDetailDto(
                slot.getId(), TODAY.plusDays(3), TEN, LocalTime.of(11, 0), 4, 3, true, false, List.of()));
        assertThat(detail.bookings()).extracting(BookingRowDto::id)
                .containsExactly(sara.getId(), omar.getId(), laila.getId());
        assertThat(detail.bookings().get(0)).isEqualTo(new BookingRowDto(sara.getId(), sara.getMember().getId(),
                "Sara Mahmoud", "01012345678", ahmed.getId(), "Ahmed Fathy", true, "upcoming", null, null));
        assertThat(detail.bookings()).extracting(BookingRowDto::status).containsOnly("upcoming");
    }

    @Test
    void fromItsStartTimeASlotIsStartedAndItsUpcomingBookingsAreWaiting() {
        Session slot = slot(TODAY, NINE, 4);
        book(slot, ahmed);

        SlotDetailDto detail = service.slot(slot.getId(), ahmed.getId());

        assertThat(detail.started()).isTrue();
        assertThat(detail.open()).isFalse();
        assertThat(detail.bookings()).extracting(BookingRowDto::status).containsExactly("waiting");
    }

    @Test
    void resultsShowTheLevelOnlyWhenPlacedAndCancelledBookingsAreLeftOut() {
        Session slot = slot(TODAY.minusWeeks(1), TEN, 4);
        AssessmentBooking placed = placed(book(slot, ahmed), "Good seat");
        AssessmentBooking noShow = noShow(book(slot, ahmed));
        cancelledBooking(slot);

        SlotDetailDto detail = service.slot(slot.getId(), ahmed.getId());

        assertThat(detail.booked()).isEqualTo(2);
        assertThat(detail.bookings()).extracting(BookingRowDto::id, BookingRowDto::status, BookingRowDto::levelId,
                BookingRowDto::notes).containsExactly(
                tuple(placed.getId(), "placed", 3, "Good seat"),
                tuple(noShow.getId(), "noshow", null, "Did not come"));
    }

    @Test
    void anotherTrainerSeesNamesButNoPhoneOrNotes() {
        Session slot = slot(TODAY.minusWeeks(1), TEN, 4);
        placed(book(slot, ahmed, "Sara Mahmoud", "01012345678"), "Good seat");
        book(slot, mona, "Laila Hassan", "01012345679");

        List<BookingRowDto> rows = service.slot(slot.getId(), mona.getId()).bookings();

        assertThat(rows.get(0)).extracting(BookingRowDto::mine, BookingRowDto::memberName, BookingRowDto::memberPhone,
                BookingRowDto::assessorName, BookingRowDto::notes, BookingRowDto::levelId)
                .containsExactly(false, "Sara Mahmoud", null, "Ahmed Fathy", null, 3);
        assertThat(rows.get(1)).extracting(BookingRowDto::mine, BookingRowDto::memberPhone, BookingRowDto::status)
                .containsExactly(true, "01012345679", "waiting");
    }

    @Test
    void aSlotWithoutBookingsHasAnEmptyList() {
        Session slot = slot(TODAY.plusDays(3), TEN, 4);

        SlotDetailDto detail = service.slot(slot.getId(), ahmed.getId());

        assertThat(detail.booked()).isZero();
        assertThat(detail.bookings()).isEmpty();
    }

    @Test
    void missingLevelAndCancelledSlotsAreNotFound() {
        Session cancelled = slot(TODAY.plusDays(3), TEN, 4);
        cancelled.cancel(NOW);
        sessionRepository.saveAndFlush(cancelled);

        assertRefused(() -> service.slot(UUID.randomUUID(), ahmed.getId()), HttpStatus.NOT_FOUND, ErrorCodes.SLOT_NOT_FOUND);
        assertRefused(() -> service.slot(levelSession(TODAY).getId(), ahmed.getId()),
                HttpStatus.NOT_FOUND, ErrorCodes.SLOT_NOT_FOUND);
        assertRefused(() -> service.slot(cancelled.getId(), ahmed.getId()),
                HttpStatus.NOT_FOUND, ErrorCodes.SLOT_NOT_FOUND);
    }

    @Test
    void theListAndTheDetailAgreeOnBookedAndOpen() {
        Session slot = slot(TODAY.plusDays(3), TEN, 2);
        book(slot, ahmed);
        book(slot, mona);
        cancelledBooking(slot);

        AssessmentSlotDto listed = flat(service.slots(null, null)).get(0);
        SlotDetailDto detail = service.slot(slot.getId(), ahmed.getId());

        assertThat(detail.booked()).isEqualTo(listed.booked()).isEqualTo(2);
        assertThat(detail.open()).isEqualTo(listed.open()).isFalse();
    }

    // ---- helpers ----

    private static List<UUID> slotIds(AssessmentSlotsDto result) {
        return flat(result).stream().map(AssessmentSlotDto::id).toList();
    }

    private static List<AssessmentSlotDto> flat(AssessmentSlotsDto result) {
        return result.days().stream().flatMap(day -> day.slots().stream()).toList();
    }

    private void assertRefused(Runnable call, HttpStatus status, String code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class, ex -> {
            assertThat(ex.status()).isEqualTo(status);
            assertThat(ex.code()).isEqualTo(code);
        });
    }

    private AssessmentBooking book(Session slot, Trainer assessor) {
        return book(slot, assessor, "Member", null);
    }

    private AssessmentBooking book(Session slot, Trainer assessor, String name, String phone) {
        Member member = memberRepository.saveAndFlush(TestMembers.member(name, phone));
        AssessmentBooking booking = bookingRepository.saveAndFlush(AssessmentBooking.book(member, slot, assessor));
        em.createNativeQuery("update assessment_booking set created_at = :at where id = :id")
                .setParameter("at", NOW.minusSeconds(3600).plusSeconds(bookingsMade++))
                .setParameter("id", booking.getId())
                .executeUpdate();
        return booking;
    }

    private AssessmentBooking placed(AssessmentBooking booking, String notes) {
        booking.recordAttended(levelRepository.getReferenceById((short) 3), notes, NOW);
        return bookingRepository.saveAndFlush(booking);
    }

    private AssessmentBooking noShow(AssessmentBooking booking) {
        booking.recordNoShow("Did not come", NOW);
        return bookingRepository.saveAndFlush(booking);
    }

    private void cancelledBooking(Session slot) {
        AssessmentBooking booking = book(slot, ahmed);
        booking.cancel(NOW);
        bookingRepository.saveAndFlush(booking);
    }

    // Each slot gets its own template: a template has at most one session per date.
    private Session slot(LocalDate date, LocalTime start, int capacity) {
        return sessionRepository.saveAndFlush(Session.of(template(null, start, capacity), date));
    }

    private Session levelSession(LocalDate date) {
        return sessionRepository.saveAndFlush(Session.of(template((short) 2, TEN, 6), date));
    }

    private SessionTemplate template(Short levelId, LocalTime start, int capacity) {
        SessionTemplate template = new SessionTemplate();
        template.setLevel(levelId == null ? null : levelRepository.getReferenceById(levelId));
        template.setDayOfWeek(DayOfWeek.SATURDAY);
        template.setStartTime(start);
        template.setEndTime(start.plusHours(1));
        template.setCapacity(capacity);
        template.setValidFrom(LocalDate.of(2026, 9, 1));
        return sessionTemplateRepository.saveAndFlush(template);
    }

    private Trainer trainer(String name, String phone, String email) {
        Trainer trainer = new Trainer();
        trainer.setName(name);
        trainer.setPhone(phone);
        trainer.setEmail(email);
        return trainerRepository.saveAndFlush(trainer);
    }

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
