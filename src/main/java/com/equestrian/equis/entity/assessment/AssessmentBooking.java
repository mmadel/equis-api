package com.equestrian.equis.entity.assessment;

import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.levels.Level;
import com.equestrian.equis.entity.sessions.Session;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

// One member booked into one assessment slot, plus the result.
// The moves only protect the entity's own state; the booking rules turn IllegalStateException into a 409.
@Entity
@Table(name = "assessment_booking")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentBooking {
    private static final String WAITING = "waiting";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    // Always an assessment slot; the service checks that. Changed only by a reschedule.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    // Changed only by the admin (W2).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessor_id", nullable = false)
    private Trainer assessor;

    @Column(name = "status", nullable = false, length = 16)
    private BookingStatus status = BookingStatus.UPCOMING;

    // Null until a result is recorded.
    @Column(name = "attended")
    private Boolean attended;

    // Set only when the member is placed.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id")
    private Level level;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "recorded_at")
    private Instant recordedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static AssessmentBooking book(Member member, Session slot, Trainer assessor) {
        AssessmentBooking booking = new AssessmentBooking();
        booking.member = member;
        booking.session = slot;
        booking.assessor = assessor;
        booking.status = BookingStatus.UPCOMING;
        return booking;
    }

    public void recordAttended(Level level, String notes, Instant now) {
        requireUpcoming("record a result for");
        this.status = BookingStatus.PLACED;
        this.attended = true;
        this.level = Objects.requireNonNull(level, "level");
        this.notes = notes;
        this.recordedAt = now;
    }

    public void recordNoShow(String notes, Instant now) {
        requireUpcoming("record a result for");
        this.status = BookingStatus.NOSHOW;
        this.attended = false;
        this.notes = notes;
        this.recordedAt = now;
    }

    // Cancelling an already cancelled booking keeps the first cancelledAt.
    public void cancel(Instant now) {
        if (status == BookingStatus.CANCELLED) {
            return;
        }
        requireUpcoming("cancel");
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = now;
    }

    // Reschedule: the same booking, in another slot. The service checks the slot and its seats.
    public void moveTo(Session slot) {
        requireUpcoming("reschedule");
        this.session = Objects.requireNonNull(slot, "slot");
    }

    // Allowed until a result places the member; a no-show or cancelled booking keeps its assessor for a rebook.
    public void changeAssessor(Trainer trainer) {
        if (status == BookingStatus.PLACED) {
            throw new IllegalStateException("Cannot change the assessor of a placed booking");
        }
        this.assessor = Objects.requireNonNull(trainer, "trainer");
    }

    // The slot has started but no result is recorded yet.
    public boolean isWaiting(Instant now, ZoneId zone) {
        return status == BookingStatus.UPCOMING && !session.startsAt(zone).toInstant().isAfter(now);
    }

    // What A3, A4 and W2 show: "waiting", or the stored status.
    public String displayStatus(Instant now, ZoneId zone) {
        return isWaiting(now, zone) ? WAITING : status.value();
    }

    private void requireUpcoming(String action) {
        if (status != BookingStatus.UPCOMING) {
            throw new IllegalStateException("Cannot " + action + " a " + status.value() + " booking");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AssessmentBooking other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    // Constant so the hash does not change when the id is generated on save.
    @Override
    public int hashCode() {
        return AssessmentBooking.class.hashCode();
    }
}
