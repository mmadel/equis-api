package com.equestrian.equis.entity.sessions;

import lombok.Setter;
import org.hibernate.annotations.TenantId;
import jakarta.persistence.EntityListeners;
import com.equestrian.equis.common.tenant.TenantGuard;
import com.equestrian.equis.entity.accounts.Trainer;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

// A rider's seat in one dated session.
@Entity
@EntityListeners(TenantGuard.class)
@Table(name = "assignment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // The club this row belongs to. Hibernate fills it from the current club on insert and adds
    // "club_id = <club>" to every query (TenantIdentifierResolver).
    @TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UUID clubId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rider_id", nullable = false, updatable = false)
    private Rider rider;

    // The trainer who gave the rider the seat.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by", nullable = false, updatable = false)
    private Trainer assignedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Assignment of(Session session, Rider rider, Trainer trainer) {
        Assignment assignment = new Assignment();
        assignment.session = session;
        assignment.rider = rider;
        assignment.assignedBy = trainer;
        return assignment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Assignment other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    // Constant so the hash does not change when the id is generated on save.
    @Override
    public int hashCode() {
        return Assignment.class.hashCode();
    }
}
