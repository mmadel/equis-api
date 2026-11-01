package com.equestrian.equis.repository.assessment;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.BookingStatus;
import com.equestrian.equis.entity.assessment.CancelReason;
import com.equestrian.equis.entity.assessment.Member;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import com.equestrian.equis.support.TestMembers;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AssessmentBookingRepositoryTest {
    private static final short LEVEL_1 = 1;
    private static final Instant NOW = Instant.parse("2026-10-05T14:00:00Z");

    // 2026-10-05 is a Monday.
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

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
    RiderRepository riderRepository;
    @Autowired
    LevelRepository levelRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    EntityManager entityManager;

    Trainer sara;
    Member omar;
    Member hana;
    SessionTemplate template;

    @BeforeEach
    void setUp() {
        sara = trainerRepository.saveAndFlush(trainer());
        omar = memberRepository.saveAndFlush(TestMembers.member("Omar Khaled", "01012345678"));
        hana = memberRepository.saveAndFlush(TestMembers.member("Hana Samy"));
        template = template();
    }

    @Test
    void registerSavesWithGeneratedIdAndTimestamps() {
        assertThat(omar.getId()).isNotNull();
        assertThat(omar.getJoinedAt()).isNotNull();
        assertThat(omar.getUpdatedAt()).isNotNull();
    }

    @Test
    void bookSavesAnUpcomingBookingAsStored() {
        AssessmentBooking saved = bookingRepository.saveAndFlush(AssessmentBooking.book(omar, slot(MONDAY), sara));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM assessment_booking WHERE id = ?", String.class,
                saved.getId())).isEqualTo("upcoming");
    }

    @Test
    void everyMoveSatisfiesTheResultCheck() {
        // One session per template and date, so both results share the Monday slot.
        Session monday = slot(MONDAY);
        AssessmentBooking placed = bookingRepository.saveAndFlush(AssessmentBooking.book(omar, monday, sara));
        placed.recordAttended(levelRepository.getReferenceById(LEVEL_1), "Steady", NOW);
        bookingRepository.saveAndFlush(placed);
        AssessmentBooking noShow = bookingRepository.saveAndFlush(AssessmentBooking.book(hana, monday, sara));
        noShow.recordNoShow(null, NOW);
        bookingRepository.saveAndFlush(noShow);
        AssessmentBooking cancelled = bookingRepository.saveAndFlush(
                AssessmentBooking.book(hana, slot(MONDAY.plusWeeks(1)), sara));
        cancelled.cancel(NOW);
        bookingRepository.saveAndFlush(cancelled);
        entityManager.clear();

        assertThat(bookingRepository.findById(placed.getId())).get().satisfies(b -> {
            assertThat(b.getStatus()).isEqualTo(BookingStatus.PLACED);
            assertThat(b.getVersion()).isEqualTo(1);
        });
        assertThat(bookingRepository.findById(noShow.getId())).get()
                .extracting(AssessmentBooking::getStatus).isEqualTo(BookingStatus.NOSHOW);
        assertThat(bookingRepository.findById(cancelled.getId())).get()
                .extracting(AssessmentBooking::getStatus).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void aMemberHasOneUpcomingBooking() {
        bookingRepository.saveAndFlush(AssessmentBooking.book(omar, slot(MONDAY), sara));

        assertThatThrownBy(() -> bookingRepository.saveAndFlush(
                AssessmentBooking.book(omar, slot(MONDAY.plusWeeks(1)), sara)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void oldBookingsStayAsHistoryAndDoNotBlockANewOne() {
        Session slot = slot(MONDAY);
        AssessmentBooking cancelled = bookingRepository.saveAndFlush(AssessmentBooking.book(omar, slot, sara));
        cancelled.cancel(NOW);
        bookingRepository.saveAndFlush(cancelled);
        AssessmentBooking noShow = bookingRepository.saveAndFlush(AssessmentBooking.book(omar, slot, sara));
        noShow.recordNoShow(null, NOW);
        bookingRepository.saveAndFlush(noShow);

        AssessmentBooking again = bookingRepository.saveAndFlush(
                AssessmentBooking.book(omar, slot(MONDAY.plusWeeks(1)), sara));

        assertThat(bookingRepository.findByMemberIdAndStatus(omar.getId(), BookingStatus.UPCOMING)).contains(again);
    }

    @Test
    void findsASlotsBookingsWithTheirMembersOldestFirst() {
        Session slot = slot(MONDAY);
        AssessmentBooking first = bookingRepository.saveAndFlush(AssessmentBooking.book(omar, slot, sara));
        AssessmentBooking second = bookingRepository.saveAndFlush(AssessmentBooking.book(hana, slot, sara));
        bookingRepository.saveAndFlush(AssessmentBooking.book(
                memberRepository.saveAndFlush(TestMembers.member("Mona Adel")), slot(MONDAY.plusWeeks(1)), sara));
        entityManager.clear();

        List<AssessmentBooking> found = bookingRepository.findBySessionId(slot.getId());

        assertThat(found).containsExactly(first, second);
        assertThat(found).allSatisfy(b -> assertThat(Hibernate.isInitialized(b.getMember())).isTrue());
    }

    @Test
    void countsAndFindsByStatus() {
        Session slot = slot(MONDAY);
        Session other = slot(MONDAY.plusWeeks(1));
        AssessmentBooking upcoming = bookingRepository.saveAndFlush(AssessmentBooking.book(omar, slot, sara));
        AssessmentBooking cancelled = bookingRepository.saveAndFlush(AssessmentBooking.book(hana, slot, sara));
        cancelled.cancel(NOW);
        bookingRepository.saveAndFlush(cancelled);
        AssessmentBooking elsewhere = bookingRepository.saveAndFlush(AssessmentBooking.book(hana, other, sara));

        assertThat(bookingRepository.countBySessionIdAndStatus(slot.getId(), BookingStatus.UPCOMING)).isEqualTo(1);
        assertThat(bookingRepository.findByMemberIdAndStatus(omar.getId(), BookingStatus.UPCOMING)).contains(upcoming);
        assertThat(bookingRepository.findByMemberIdAndStatus(hana.getId(), BookingStatus.UPCOMING))
                .contains(elsewhere);
        assertThat(bookingRepository.findBySessionIdInAndStatus(List.of(slot.getId(), other.getId()),
                BookingStatus.UPCOMING)).containsExactlyInAnyOrder(upcoming, elsewhere);
    }

    @Test
    void countsUpcomingBookingsPerUpcomingScheduledSessionOfTheTemplate() {
        Session past = slot(MONDAY.minusWeeks(1));
        Session today = slot(MONDAY);
        Session nextWeek = slot(MONDAY.plusWeeks(1));
        Session cancelledSlot = Session.of(template, MONDAY.plusWeeks(2));
        cancelledSlot.cancel(NOW);
        sessionRepository.saveAndFlush(cancelledSlot);
        slot(MONDAY.plusWeeks(3));
        Member mona = memberRepository.saveAndFlush(TestMembers.member("Mona Adel"));
        Member laila = memberRepository.saveAndFlush(TestMembers.member("Laila Nour"));
        Member nour = memberRepository.saveAndFlush(TestMembers.member("Nour Adel"));
        bookingRepository.saveAndFlush(AssessmentBooking.book(omar, past, sara));
        bookingRepository.saveAndFlush(AssessmentBooking.book(hana, today, sara));
        bookingRepository.saveAndFlush(AssessmentBooking.book(mona, nextWeek, sara));
        bookingRepository.saveAndFlush(AssessmentBooking.book(laila, nextWeek, sara));
        AssessmentBooking noShow = bookingRepository.saveAndFlush(AssessmentBooking.book(nour, nextWeek, sara));
        noShow.recordNoShow(null, NOW);
        bookingRepository.saveAndFlush(noShow);
        bookingRepository.saveAndFlush(AssessmentBooking.book(nour, cancelledSlot, sara));

        assertThat(bookingRepository.countUpcomingPerUpcomingSession(template.getId(), MONDAY, LocalTime.of(15, 59)))
                .containsExactlyInAnyOrder(1L, 2L);
        // A slot starting exactly now is no longer upcoming.
        assertThat(bookingRepository.countUpcomingPerUpcomingSession(template.getId(), MONDAY, LocalTime.of(16, 0)))
                .containsExactly(2L);
    }

    // E11 cancel reason, case 1: the columns exist (the context only starts if validate accepts the entity).
    @Test
    void cancelReasonColumnsExist() {
        assertThat(jdbcTemplate.queryForList("""
                select column_name from information_schema.columns
                where table_name = 'assessment_booking' and column_name like 'cancel_reason%'""", String.class))
                .containsExactlyInAnyOrder("cancel_reason", "cancel_reason_text");
    }

    // E11 cancel reason, cases 2 and 3: stored as cancelled, with the reason and the trimmed text.
    @Test
    void cancelWithReasonIsStored() {
        Session monday = slot(MONDAY);
        AssessmentBooking unwell = bookingRepository.saveAndFlush(AssessmentBooking.book(omar, monday, sara));
        unwell.cancelWithReason(CancelReason.PERSON_UNWELL, null, NOW);
        bookingRepository.saveAndFlush(unwell);
        AssessmentBooking other = bookingRepository.saveAndFlush(AssessmentBooking.book(hana, monday, sara));
        other.cancelWithReason(CancelReason.OTHER, "  Family travel ", NOW);
        bookingRepository.saveAndFlush(other);
        entityManager.clear();

        assertThat(jdbcTemplate.queryForMap(
                "SELECT status, cancel_reason, cancel_reason_text FROM assessment_booking WHERE id = ?", unwell.getId()))
                .containsEntry("status", "cancelled")
                .containsEntry("cancel_reason", "person_unwell")
                .containsEntry("cancel_reason_text", null);
        assertThat(bookingRepository.findById(unwell.getId())).get().satisfies(b -> {
            assertThat(b.getCancelReason()).isEqualTo(CancelReason.PERSON_UNWELL);
            assertThat(b.getCancelledAt()).isEqualTo(NOW);
        });
        assertThat(bookingRepository.findById(other.getId())).get().satisfies(b -> {
            assertThat(b.getCancelReason()).isEqualTo(CancelReason.OTHER);
            assertThat(b.getCancelReasonText()).isEqualTo("Family travel");
        });
    }

    // E11 cancel reason, case 7: the database refuses a reason that doesn't fit.
    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "upcoming,  weather, NULL,          ck_assessment_booking_cancel_status",
            "cancelled, sick,    NULL,          ck_assessment_booking_cancel_reason",
            "cancelled, other,   NULL,          ck_assessment_booking_cancel_text",
            "cancelled, other,   '  ',          ck_assessment_booking_cancel_text",
            "cancelled, weather, Stormy,        ck_assessment_booking_cancel_text"})
    void reasonsThatDoNotFitAreRefused(String status, String reason, String text, String constraint) {
        Session slot = slot(MONDAY);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO assessment_booking (member_id, session_id, assessor_id, status, cancelled_at,
                                                cancel_reason, cancel_reason_text, created_at, updated_at)
                VALUES (?, ?, ?, ?, CASE WHEN ?::text = 'cancelled' THEN now() END, ?, ?, now(), now())""",
                omar.getId(), slot.getId(), sara.getId(), status, status, reason, text))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(constraint);
    }

    @Test
    void riderKnowsWhetherAMemberWasPlaced() {
        Rider rider = new Rider();
        rider.setName("Omar Khaled");
        rider.setLevel(levelRepository.getReferenceById(LEVEL_1));
        rider.setMemberId(omar.getId());
        riderRepository.saveAndFlush(rider);

        assertThat(riderRepository.existsByMemberId(omar.getId())).isTrue();
        assertThat(riderRepository.existsByMemberId(hana.getId())).isFalse();
        assertThat(riderRepository.existsByMemberId(UUID.randomUUID())).isFalse();
    }

    private Session slot(LocalDate date) {
        return sessionRepository.saveAndFlush(Session.of(template, date));
    }

    // An assessment template: no level.
    private SessionTemplate template() {
        SessionTemplate template = new SessionTemplate();
        template.setDayOfWeek(DayOfWeek.MONDAY);
        template.setStartTime(LocalTime.of(16, 0));
        template.setEndTime(LocalTime.of(17, 0));
        template.setCapacity(4);
        template.setValidFrom(LocalDate.of(2026, 9, 1));
        return sessionTemplateRepository.saveAndFlush(template);
    }

    private Trainer trainer() {
        Trainer trainer = new Trainer();
        trainer.setName("Sara Ali");
        trainer.setPhone("01999990001");
        trainer.setEmail("booking-test-sara@example.com");
        return trainer;
    }
}
