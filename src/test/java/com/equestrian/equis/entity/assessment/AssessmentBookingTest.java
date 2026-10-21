package com.equestrian.equis.entity.assessment;

import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.levels.Level;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.support.TestMembers;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssessmentBookingTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // 2026-10-05 is a Monday; the slot starts at 16:00 Cairo time (13:00 UTC).
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);
    private static final Instant SLOT_START = Instant.parse("2026-10-05T13:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-05T14:00:00Z");

    private final Level level = new Level();

    @Test
    void bookStartsUpcoming() {
        Member member = TestMembers.member("Omar");
        Trainer assessor = new Trainer();
        Session slot = slot();

        AssessmentBooking booking = AssessmentBooking.book(member, slot, assessor);

        assertThat(booking.getMember()).isSameAs(member);
        assertThat(booking.getSession()).isSameAs(slot);
        assertThat(booking.getAssessor()).isSameAs(assessor);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.UPCOMING);
        assertThat(booking.getAttended()).isNull();
        assertThat(booking.getLevel()).isNull();
    }

    @Test
    void recordAttendedPlacesTheMember() {
        AssessmentBooking booking = booking();

        booking.recordAttended(level, "Good seat", NOW);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PLACED);
        assertThat(booking.getAttended()).isTrue();
        assertThat(booking.getLevel()).isSameAs(level);
        assertThat(booking.getNotes()).isEqualTo("Good seat");
        assertThat(booking.getRecordedAt()).isEqualTo(NOW);
        assertThat(booking.getCancelledAt()).isNull();
    }

    @Test
    void recordAttendedNeedsALevel() {
        assertThatThrownBy(() -> booking().recordAttended(null, null, NOW)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void recordNoShowLeavesTheLevelEmpty() {
        AssessmentBooking booking = booking();

        booking.recordNoShow("Did not come", NOW);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.NOSHOW);
        assertThat(booking.getAttended()).isFalse();
        assertThat(booking.getLevel()).isNull();
        assertThat(booking.getNotes()).isEqualTo("Did not come");
        assertThat(booking.getRecordedAt()).isEqualTo(NOW);
    }

    @Test
    void cancelKeepsTheFirstCancelledAt() {
        AssessmentBooking booking = booking();

        booking.cancel(NOW);
        booking.cancel(NOW.plusSeconds(60));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelledAt()).isEqualTo(NOW);
        assertThat(booking.getAttended()).isNull();
        assertThat(booking.getRecordedAt()).isNull();
    }

    @Test
    void movesFromAnythingButUpcomingAreRejected() {
        AssessmentBooking placed = booking();
        placed.recordAttended(level, null, NOW);
        AssessmentBooking noShow = booking();
        noShow.recordNoShow(null, NOW);
        AssessmentBooking cancelled = booking();
        cancelled.cancel(NOW);

        for (AssessmentBooking booking : new AssessmentBooking[] {placed, noShow}) {
            assertThatThrownBy(() -> booking.recordAttended(level, null, NOW)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> booking.recordNoShow(null, NOW)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> booking.cancel(NOW)).isInstanceOf(IllegalStateException.class);
        }
        assertThatThrownBy(() -> cancelled.recordAttended(level, null, NOW)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cancelled.recordNoShow(null, NOW)).isInstanceOf(IllegalStateException.class);
    }

    // Cancel reason, case 2 (entity part)
    @Test
    void cancelWithReasonStoresTheReason() {
        AssessmentBooking booking = booking();

        booking.cancelWithReason(CancelReason.PERSON_UNWELL, null, NOW);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelReason()).isEqualTo(CancelReason.PERSON_UNWELL);
        assertThat(booking.getCancelReasonText()).isNull();
        assertThat(booking.getCancelledAt()).isEqualTo(NOW);
    }

    // Cancel reason, case 3
    @Test
    void cancelForAnotherReasonTrimsTheText() {
        AssessmentBooking booking = booking();

        booking.cancelWithReason(CancelReason.OTHER, "  Family travel ", NOW);

        assertThat(booking.getCancelReason()).isEqualTo(CancelReason.OTHER);
        assertThat(booking.getCancelReasonText()).isEqualTo("Family travel");
    }

    // Cancel reason, case 4
    @Test
    void textMustMatchTheReason() {
        AssessmentBooking booking = booking();

        assertThatThrownBy(() -> booking.cancelWithReason(CancelReason.OTHER, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> booking.cancelWithReason(CancelReason.OTHER, "   ", NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> booking.cancelWithReason(CancelReason.WEATHER, "Storm", NOW))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.UPCOMING);
        assertThat(booking.getCancelReason()).isNull();
        assertThat(booking.getCancelReasonText()).isNull();
        assertThat(booking.getCancelledAt()).isNull();
    }

    // Cancel reason, case 5
    @Test
    void cancelWithReasonOnlyFromUpcoming() {
        AssessmentBooking placed = booking();
        placed.recordAttended(level, null, NOW);
        AssessmentBooking noShow = booking();
        noShow.recordNoShow(null, NOW);
        AssessmentBooking cancelled = booking();
        cancelled.cancel(NOW);

        for (AssessmentBooking booking : new AssessmentBooking[] {placed, noShow, cancelled}) {
            assertThatThrownBy(() -> booking.cancelWithReason(CancelReason.WEATHER, null, NOW))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(booking.getCancelReason()).isNull();
        }
    }

    // Cancel reason, case 6 (entity part): the slot-delete cancel stores no reason.
    @Test
    void plainCancelLeavesTheReasonEmpty() {
        AssessmentBooking booking = booking();

        booking.cancel(NOW);

        assertThat(booking.getCancelReason()).isNull();
        assertThat(booking.getCancelReasonText()).isNull();
    }

    @Test
    void anUpcomingBookingIsWaitingOnceTheSlotStarts() {
        AssessmentBooking booking = booking();

        assertThat(booking.isWaiting(SLOT_START.minusSeconds(1), CAIRO)).isFalse();
        assertThat(booking.displayStatus(SLOT_START.minusSeconds(1), CAIRO)).isEqualTo("upcoming");
        assertThat(booking.isWaiting(SLOT_START, CAIRO)).isTrue();
        assertThat(booking.displayStatus(SLOT_START, CAIRO)).isEqualTo("waiting");
    }

    @Test
    void aRecordedBookingShowsItsStoredStatus() {
        AssessmentBooking placed = booking();
        placed.recordAttended(level, null, NOW);
        AssessmentBooking noShow = booking();
        noShow.recordNoShow(null, NOW);
        AssessmentBooking cancelled = booking();
        cancelled.cancel(NOW);

        assertThat(placed.isWaiting(NOW, CAIRO)).isFalse();
        assertThat(placed.displayStatus(NOW, CAIRO)).isEqualTo("placed");
        assertThat(noShow.displayStatus(NOW, CAIRO)).isEqualTo("noshow");
        assertThat(cancelled.displayStatus(NOW, CAIRO)).isEqualTo("cancelled");
    }

    private AssessmentBooking booking() {
        return AssessmentBooking.book(TestMembers.member("Omar"), slot(), new Trainer());
    }

    private Session slot() {
        SessionTemplate template = new SessionTemplate();
        template.setDayOfWeek(DayOfWeek.MONDAY);
        template.setStartTime(LocalTime.of(16, 0));
        template.setEndTime(LocalTime.of(17, 0));
        template.setCapacity(4);
        template.setValidFrom(MONDAY);
        return Session.of(template, MONDAY);
    }
}
