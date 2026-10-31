package com.equestrian.equis.entity.sessions;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

// A dated occurrence of a session template. Not org.hibernate.Session.
@Entity
@Table(name = "session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false, updatable = false)
    private SessionTemplate template;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "status", nullable = false, length = 16)
    private SessionStatus status = SessionStatus.SCHEDULED;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    // The trainer's id; the foreign key is in the database.
    @Column(name = "finished_by")
    private UUID finishedBy;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Session of(SessionTemplate t, LocalDate date) {
        Session session = new Session();
        session.template = t;
        session.sessionDate = date;
        session.status = SessionStatus.SCHEDULED;
        session.applyTemplate(t);
        return session;
    }

    public ZonedDateTime startsAt(ZoneId zone) {
        return sessionDate.atTime(startTime).atZone(zone);
    }

    public boolean isUpcoming(Instant now, ZoneId zone) {
        return startsAt(zone).toInstant().isAfter(now);
    }

    public boolean hasStarted(Instant now, ZoneId zone) {
        return !now.isBefore(startsAt(zone).toInstant());
    }

    // A done session has been finished too.
    public boolean isFinished() {
        return status == SessionStatus.FINISHED || status == SessionStatus.DONE;
    }

    public boolean isDone() {
        return status == SessionStatus.DONE;
    }

    // A finished session is final. The start time is checked by the finish endpoint, which knows the school zone.
    public void finish(UUID trainerId, Instant now) {
        if (status != SessionStatus.SCHEDULED) {
            throw new IllegalStateException("Only a scheduled session can be finished");
        }
        status = SessionStatus.FINISHED;
        finishedBy = Objects.requireNonNull(trainerId, "trainerId");
        finishedAt = Objects.requireNonNull(now, "now");
    }

    // A finished session becomes done once every seated rider has an evaluation. Returns whether it changed.
    public boolean markDoneIfAllEvaluated(long seated, long evaluated) {
        if (status != SessionStatus.FINISHED || evaluated < seated) {
            return false;
        }
        status = SessionStatus.DONE;
        return true;
    }

    // Cancelling an already cancelled session keeps the first cancelledAt. A finished session can't be cancelled.
    public void cancel(Instant now) {
        if (status == SessionStatus.CANCELLED) {
            return;
        }
        if (isFinished()) {
            throw new IllegalStateException("A finished session can't be cancelled");
        }
        status = SessionStatus.CANCELLED;
        cancelledAt = now;
    }

    // Re-copies the template's time and capacity; used when a template is edited.
    public void applyTemplate(SessionTemplate t) {
        startTime = t.getStartTime();
        endTime = t.getEndTime();
        capacity = t.getCapacity();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Session other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    // Constant so the hash does not change when the id is generated on save.
    @Override
    public int hashCode() {
        return Session.class.hashCode();
    }
}
