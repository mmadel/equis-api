package com.equestrian.equis.entity.payments;

import com.equestrian.equis.entity.riders.Rider;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// One session a rider used, and the package it came out of. A rider uses a session at most once, and the rider is
// always the package's rider (composite foreign key). Written when a finished session is deducted (next epic).
@Entity
@Table(name = "session_usage")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rider_id", nullable = false, updatable = false)
    private Rider rider;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, updatable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    private Session session;

    @Column(name = "used_at", nullable = false, updatable = false)
    private Instant usedAt;

    public static SessionUsage of(Payment payment, Session session, Instant now) {
        SessionUsage usage = new SessionUsage();
        usage.payment = Objects.requireNonNull(payment, "payment");
        usage.rider = payment.getRider();
        usage.session = Objects.requireNonNull(session, "session");
        usage.usedAt = Objects.requireNonNull(now, "now");
        return usage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SessionUsage other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    // Constant so the hash does not change when the id is generated on save.
    @Override
    public int hashCode() {
        return SessionUsage.class.hashCode();
    }
}
