package com.equestrian.equis.entity.riders;

import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.levels.Level;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// One promotion or demotion of a rider. History only: rows are never updated or deleted.
// The factory only protects the row; the level change rules turn bad input into proper errors first.
@Entity
@Table(name = "level_change")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LevelChange {
    public static final int REASON_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rider_id", nullable = false, updatable = false)
    private Rider rider;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_level_id", nullable = false, updatable = false)
    private Level fromLevel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_level_id", nullable = false, updatable = false)
    private Level toLevel;

    @Column(name = "reason", nullable = false, updatable = false, length = REASON_MAX_LENGTH)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "by_trainer_id", nullable = false, updatable = false)
    private Trainer byTrainer;

    // From the app clock, not @CreationTimestamp: the history is ordered by it, and the VM clock behind
    // @CreationTimestamp ticks only every ~15 ms on Windows, so two quick changes could tie.
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static LevelChange of(Rider rider, Level from, Level to, String reason, Trainer by, Instant at) {
        if (!isOneStep(from, to)) {
            throw new IllegalArgumentException("A level change must move exactly one level");
        }
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("A level change needs a reason");
        }
        LevelChange change = new LevelChange();
        change.rider = rider;
        change.fromLevel = from;
        change.toLevel = to;
        change.reason = trimmed;
        change.byTrainer = by;
        change.createdAt = Objects.requireNonNull(at, "at");
        return change;
    }

    public boolean isPromotion() {
        return toLevel.getId() > fromLevel.getId();
    }

    // Reads only the ids, so level references are not loaded.
    static boolean isOneStep(Level from, Level to) {
        return from != null && to != null && Math.abs(to.getId() - from.getId()) == 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LevelChange other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    // Constant so the hash does not change when the id is generated on save.
    @Override
    public int hashCode() {
        return LevelChange.class.hashCode();
    }
}
