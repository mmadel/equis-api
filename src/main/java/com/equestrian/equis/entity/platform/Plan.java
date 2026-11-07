package com.equestrian.equis.entity.platform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.OptionalInt;
import java.util.UUID;

// What a club may do while a subscription to it runs. Global, like club: no @TenantId, no RLS.
// Rows come only from ops/upsert-plan.sql, so the app never writes them. Behaviour comes from the columns,
// never from the code: the trial plan is the one with trialDays set.
@Entity
@Table(name = "plan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    // Null means unlimited.
    @Column(name = "max_trainers")
    private Integer maxTrainers;

    // Null means unlimited.
    @Column(name = "max_riders")
    private Integer maxRiders;

    // Set only on the trial plan.
    @Column(name = "trial_days")
    private Integer trialDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "on_expiry", nullable = false, length = 10)
    private OnExpiry onExpiry;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isTrial() {
        return trialDays != null;
    }

    // Empty when unlimited.
    public OptionalInt trainerLimit() {
        return maxTrainers == null ? OptionalInt.empty() : OptionalInt.of(maxTrainers);
    }

    // Empty when unlimited.
    public OptionalInt riderLimit() {
        return maxRiders == null ? OptionalInt.empty() : OptionalInt.of(maxRiders);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Plan other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Plan.class.hashCode();
    }
}
