package com.equestrian.equis.entity.reports;

import com.equestrian.equis.entity.payments.PaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// A row of the Payments report, read from the view report_payment_v (V29). Read-only.
@Entity
@Immutable
@Table(name = "report_payment_v")
@Getter
public class PaymentReportRow {
    @Id
    @Column(name = "id")
    private UUID id;

    // Hibernate adds "club_id = <club>" to every query; the view's row-level security does the same in PostgreSQL.
    @TenantId
    @Column(name = "club_id")
    private UUID clubId;

    @Column(name = "recorded_at")
    private Instant recordedAt;

    @Column(name = "rider_name")
    private String riderName;

    // The rider's trainer today; null when the rider has none.
    @Column(name = "trainer_name")
    private String trainerName;

    @Column(name = "amount_egp")
    private BigDecimal amountEgp;

    // Read through PaymentMethod.DbConverter (auto-applied).
    @Column(name = "method")
    private PaymentMethod method;

    // Always the 1st of the month.
    @Column(name = "start_month")
    private LocalDate startMonth;

    @Column(name = "months")
    private int months;

    @Column(name = "sessions")
    private int sessions;

    @Column(name = "voided")
    private boolean voided;

    // Null for a valid payment.
    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "recorded_by_name")
    private String recordedByName;

    protected PaymentReportRow() {
    }
}
