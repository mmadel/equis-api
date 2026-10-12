package com.equestrian.equis.entity.accounts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
@Getter
@Setter
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "subject_type", nullable = false, updatable = false, length = 16)
    private SubjectType subjectType;

    // Points to trainer.id or admin_account.id depending on subjectType, so it is not an association.
    @Column(name = "subject_id", nullable = false, updatable = false)
    private UUID subjectId;

    @Column(name = "token_hash", nullable = false, unique = true, updatable = false, length = 64)
    private String tokenHash;

    @Column(name = "remember_me", nullable = false, updatable = false)
    private boolean rememberMe;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private RefreshToken replacedBy;

}
