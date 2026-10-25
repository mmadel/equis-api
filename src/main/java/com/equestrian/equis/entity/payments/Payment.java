package com.equestrian.equis.entity.payments;

import com.equestrian.equis.entity.riders.Rider;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

// A package of sessions a rider paid for. Valid from the 1st of the start month to the end of the grace month,
// the month after the last paid one. Never edited or deleted: a wrong payment is voided and stops counting.
@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    public static final int SESSIONS_PER_MONTH = 8;
    public static final int MIN_MONTHS = 1;
    public static final int MAX_MONTHS = 12;
    public static final int MIN_SESSIONS = 1;
    public static final int MAX_SESSIONS = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rider_id", nullable = false, updatable = false)
    private Rider rider;

    @Column(name = "amount_egp", nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal amountEgp;

    @Column(name = "method", nullable = false, length = 20, updatable = false)
    private PaymentMethod method;

    // Always the 1st of the month.
    @Column(name = "start_month", nullable = false, updatable = false)
    private LocalDate startMonth;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "months", nullable = false, updatable = false)
    private int months;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "sessions", nullable = false, updatable = false)
    private int sessions;

    // Last day of the grace month. Stored so lists can filter and sort on it; a CHECK keeps it right.
    @Column(name = "valid_until", nullable = false, updatable = false)
    private LocalDate validUntil;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    // The admin's id; the foreign key is in the database.
    @Column(name = "recorded_by", nullable = false, updatable = false)
    private UUID recordedBy;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "voided_by")
    private UUID voidedBy;

    @Column(name = "void_reason", length = 300)
    private String voidReason;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public static Payment record(Rider rider, BigDecimal amountEgp, PaymentMethod method, YearMonth startMonth,
                                 int months, int sessions, UUID adminId, Instant now) {
        if (amountEgp == null || amountEgp.signum() <= 0) {
            throw new IllegalArgumentException("The amount must be above 0");
        }
        if (months < MIN_MONTHS || months > MAX_MONTHS) {
            throw new IllegalArgumentException("Months must be between " + MIN_MONTHS + " and " + MAX_MONTHS);
        }
        if (sessions < MIN_SESSIONS || sessions > MAX_SESSIONS) {
            throw new IllegalArgumentException("Sessions must be between " + MIN_SESSIONS + " and " + MAX_SESSIONS);
        }
        Payment payment = new Payment();
        payment.rider = Objects.requireNonNull(rider, "rider");
        payment.amountEgp = amountEgp;
        payment.method = Objects.requireNonNull(method, "method");
        payment.startMonth = startMonth.atDay(1);
        payment.months = months;
        payment.sessions = sessions;
        payment.validUntil = startMonth.plusMonths(months + 1L).atDay(1).minusDays(1);
        payment.recordedBy = Objects.requireNonNull(adminId, "adminId");
        payment.recordedAt = Objects.requireNonNull(now, "now");
        return payment;
    }

    // What the API and the form fill in when the admin doesn't change it.
    public static int defaultSessions(int months) {
        return SESSIONS_PER_MONTH * months;
    }

    public boolean isVoided() {
        return voidedAt != null;
    }

    // Counts only from its start month, through the grace month, and never once voided.
    public boolean isValidOn(LocalDate date) {
        return !isVoided() && !date.isBefore(startMonth) && !date.isAfter(validUntil);
    }

    // The reason is checked before anything changes, so a refused void leaves the payment as it was.
    public void voidPayment(UUID adminId, String reason, Instant now) {
        if (isVoided()) {
            throw new IllegalStateException("This payment is already voided");
        }
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("A void needs a reason");
        }
        this.voidedBy = Objects.requireNonNull(adminId, "adminId");
        this.voidedAt = Objects.requireNonNull(now, "now");
        this.voidReason = trimmed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Payment other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    // Constant so the hash does not change when the id is generated on save.
    @Override
    public int hashCode() {
        return Payment.class.hashCode();
    }
}
