package com.equestrian.equis.entity.reports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

// A rider of the Rider balances report, read from the view report_rider_v (V30). Read-only; the balance is worked
// out by RiderBalanceService.
@Entity
@Immutable
@Table(name = "report_rider_v")
@Getter
public class RiderReportRow {
    @Id
    @Column(name = "id")
    private UUID id;

    // Hibernate adds "club_id = <club>" to every query; the view's row-level security does the same in PostgreSQL.
    @TenantId
    @Column(name = "club_id")
    private UUID clubId;

    @Column(name = "rider_name")
    private String riderName;

    // Null when the rider has no trainer.
    @Column(name = "trainer_name")
    private String trainerName;

    protected RiderReportRow() {
    }
}
