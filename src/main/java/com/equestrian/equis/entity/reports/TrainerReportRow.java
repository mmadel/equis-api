package com.equestrian.equis.entity.reports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

// A row of the Trainers report, read from the view report_trainer_v (V28). Read-only.
@Entity
@Immutable
@Table(name = "report_trainer_v")
@Getter
public class TrainerReportRow {
    @Id
    @Column(name = "id")
    private UUID id;

    // Hibernate adds "club_id = <club>" to every query; the view's row-level security does the same in PostgreSQL.
    @TenantId
    @Column(name = "club_id")
    private UUID clubId;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    // As stored: invited, active or disabled.
    @Column(name = "status")
    private String status;

    protected TrainerReportRow() {
    }
}
