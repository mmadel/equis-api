package com.equestrian.equis.entity.accounts;

import lombok.AccessLevel;
import org.hibernate.annotations.TenantId;
import jakarta.persistence.EntityListeners;
import com.equestrian.equis.common.tenant.TenantGuard;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@EntityListeners(TenantGuard.class)
@Table(name = "admin_account")
@Getter
@Setter
public class AdminAccount implements LockableAccount {
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

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
