package com.equestrian.equis.entity.platform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// One period a club is covered by a plan. Global, like club: no @TenantId, no RLS; clubId is a plain foreign key.
// Never edited: a wrong row is voided with a reason and stays in the history.
// The club's current subscription is its latest started, non-voided row (SubscriptionRepository.findCurrent).
@Entity
@Table(name = "subscription")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {
    public static final int MIN_VOID_REASON = 5;
    public static final int MAX_TEXT = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "club_id", nullable = false, updatable = false)
    private UUID clubId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, updatable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_period", nullable = false, length = 10, updatable = false)
    private BillingPeriod billingPeriod;

    @Column(name = "starts_at", nullable = false, updatable = false)
    private Instant startsAt;

    // Exclusive: the club is covered while now < endsAt.
    @Column(name = "ends_at", nullable = false, updatable = false)
    private Instant endsAt;

    @Column(name = "note", length = MAX_TEXT, updatable = false)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // A super admin's id, with no foreign key. Null when the server made the row itself.
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "voided_at")
    private Instant voidedAt;

    // A super admin's id, with no foreign key.
    @Column(name = "voided_by")
    private UUID voidedBy;

    @Column(name = "void_reason", length = MAX_TEXT)
    private String voidReason;

    // Checks the rules a database CHECK can't (they need the plan row): TRIAL only with the trial plan, and the
    // trial plan only with TRIAL; paid plans are MONTHLY or ANNUAL.
    public static Subscription create(UUID clubId, Plan plan, BillingPeriod period, Instant startsAt,
                                      Instant endsAt, String note, UUID createdBy) {
        Objects.requireNonNull(clubId, "clubId");
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(period, "period");
        Objects.requireNonNull(startsAt, "startsAt");
        Objects.requireNonNull(endsAt, "endsAt");
        if (plan.isTrial() && period != BillingPeriod.TRIAL) {
            throw new IllegalArgumentException("The trial plan can only be a trial period");
        }
        if (!plan.isTrial() && period == BillingPeriod.TRIAL) {
            throw new IllegalArgumentException("A trial period needs the trial plan");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("The end must be after the start");
        }
        String trimmedNote = note == null || note.isBlank() ? null : note.trim();
        if (trimmedNote != null && trimmedNote.length() > MAX_TEXT) {
            throw new IllegalArgumentException("The note can be at most " + MAX_TEXT + " characters");
        }
        Subscription subscription = new Subscription();
        subscription.clubId = clubId;
        subscription.plan = plan;
        subscription.billingPeriod = period;
        subscription.startsAt = startsAt;
        subscription.endsAt = endsAt;
        subscription.note = trimmedNote;
        subscription.createdBy = createdBy;
        return subscription;
    }

    // The reason is checked before anything changes, so a refused void leaves the row as it was.
    public void voidWith(String reason, UUID by, Instant now) {
        if (isVoided()) {
            throw new IllegalStateException("This subscription is already voided");
        }
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.length() < MIN_VOID_REASON) {
            throw new IllegalArgumentException("A void needs a reason of at least " + MIN_VOID_REASON + " characters");
        }
        if (trimmed.length() > MAX_TEXT) {
            throw new IllegalArgumentException("The reason can be at most " + MAX_TEXT + " characters");
        }
        this.voidedBy = Objects.requireNonNull(by, "by");
        this.voidedAt = Objects.requireNonNull(now, "now");
        this.voidReason = trimmed;
    }

    public boolean isVoided() {
        return voidedAt != null;
    }

    // Covers the club at that moment: not voided, started, and not yet ended.
    public boolean isValidAt(Instant now) {
        return !isVoided() && !startsAt.isAfter(now) && endsAt.isAfter(now);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Subscription other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    // Constant so the hash does not change when the id is generated on save.
    @Override
    public int hashCode() {
        return Subscription.class.hashCode();
    }
}
