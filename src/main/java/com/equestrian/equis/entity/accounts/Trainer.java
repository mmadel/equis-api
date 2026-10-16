package com.equestrian.equis.entity.accounts;

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
@Table(name = "trainer")
@Getter
@Setter
public class Trainer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone", nullable = false, unique = true, length = 11)
    private String phone;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "status", nullable = false, length = 16)
    private TrainerStatus status = TrainerStatus.INVITED;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // A new trainer has no password until they open their invite link.
    public static Trainer invite(String name, String phone, String email) {
        Trainer trainer = new Trainer();
        trainer.name = name;
        trainer.phone = phone;
        trainer.email = email;
        trainer.status = TrainerStatus.INVITED;
        return trainer;
    }
}
