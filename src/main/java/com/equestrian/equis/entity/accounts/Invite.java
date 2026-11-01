package com.equestrian.equis.entity.accounts;

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
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invite")
@Getter
@Setter
public class Invite {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // Exactly one owner: a trainer or a rider (ck_invite_owner). Set only by the factories.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id", updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Setter(AccessLevel.NONE)
    private Trainer trainer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Setter(AccessLevel.NONE)
    private Rider rider;

    @Column(name = "token_hash", nullable = false, unique = true, updatable = false, length = 64)
    private String tokenHash;

    @Column(name = "purpose", nullable = false, updatable = false, length = 16)
    private InvitePurpose purpose;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Invite forTrainer(Trainer trainer) {
        if (trainer == null) {
            throw new IllegalArgumentException("A trainer link needs a trainer");
        }
        Invite invite = new Invite();
        invite.trainer = trainer;
        return invite;
    }

    public static Invite forRider(Rider rider) {
        if (rider == null) {
            throw new IllegalArgumentException("A rider link needs a rider");
        }
        Invite invite = new Invite();
        invite.rider = rider;
        return invite;
    }

    public boolean isForTrainer() {
        return trainer != null;
    }

    public boolean isForRider() {
        return rider != null;
    }

    // A link works once and only until it expires.
    public boolean isOpen(Instant now) {
        return acceptedAt == null && expiresAt.isAfter(now);
    }
}
