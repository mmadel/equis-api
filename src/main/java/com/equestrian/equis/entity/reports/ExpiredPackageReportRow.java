package com.equestrian.equis.entity.reports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

import java.time.LocalDate;
import java.util.UUID;

// A row of the Expired sessions report, read from the view report_expired_package_v (V31): a live package with
// unused sessions. Read-only.
@Entity
@Immutable
@Table(name = "report_expired_package_v")
@Getter
public class ExpiredPackageReportRow {
    @Id
    @Column(name = "id")
    private UUID id;

    // Hibernate adds "club_id = <club>" to every query; the view's row-level security does the same in PostgreSQL.
    @TenantId
    @Column(name = "club_id")
    private UUID clubId;

    @Column(name = "rider_name")
    private String riderName;

    // The rider's trainer today; null when the rider has none.
    @Column(name = "trainer_name")
    private String trainerName;

    // Always the 1st of the month.
    @Column(name = "start_month")
    private LocalDate startMonth;

    @Column(name = "months")
    private int months;

    // The last day of the grace month: the package ended at the end of this day.
    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "sessions")
    private int sessions;

    @Column(name = "used")
    private int used;

    // sessions - used, always above zero.
    @Column(name = "expired")
    private int expired;

    protected ExpiredPackageReportRow() {
    }
}
