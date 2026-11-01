package com.equestrian.equis.entity.riders;

import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.assessment.Member;
import com.equestrian.equis.entity.levels.Level;
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
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rider")
@Getter
@Setter
public class Rider {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone", length = 11)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "level_id", nullable = false)
    private Level level;

    // Null means the rider is unassigned.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id")
    private Trainer trainer;

    // The member this rider came from, if any. A plain id: the foreign key is in the database and the rider never loads the member.
    @Column(name = "member_id")
    private UUID memberId;

    // The login email, copied from the member at placement. Null for seed riders, who have no member.
    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "account_status", nullable = false, length = 16)
    private RiderAccountStatus accountStatus = RiderAccountStatus.NONE;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // The only way a real rider is created: the member placed at an assessment, assigned to the assessor.
    // The member must be loaded (its name, phone and email are copied); the trainer may be a reference.
    public static Rider fromAssessment(Member member, Level level, Trainer trainer) {
        Rider rider = new Rider();
        rider.name = member.getName();
        rider.phone = member.getPhone();
        // Already lowercase: Member stores it that way.
        rider.email = member.getEmail();
        rider.level = level;
        rider.trainer = trainer;
        rider.memberId = member.getId();
        return rider;
    }

    // The only way a rider's level changes: exactly one step up or down. The caller records the LevelChange.
    public void moveToLevel(Level to) {
        if (!LevelChange.isOneStep(level, to)) {
            throw new IllegalArgumentException("A rider moves exactly one level at a time");
        }
        this.level = to;
    }

    // The admin gives the rider to another trainer (W3). False when it is already that trainer, so nothing changes.
    // Reads only the current trainer's id, so the lazy trainer is never loaded.
    public boolean assignTrainer(Trainer to) {
        if (to == null) {
            throw new IllegalArgumentException("A rider is assigned to a trainer, never to nobody");
        }
        if (trainer != null && trainer.getId().equals(to.getId())) {
            return false;
        }
        this.trainer = to;
        return true;
    }

    // Whether the rider can sign in.
    public boolean hasAccount() {
        return accountStatus == RiderAccountStatus.ACTIVE;
    }

    // A set-password link was sent. Sending again while invited is fine; an active rider gets a reset instead.
    public void markInvited() {
        if (email == null) {
            throw new IllegalStateException("A rider without an email can't be invited");
        }
        if (accountStatus == RiderAccountStatus.ACTIVE) {
            throw new IllegalStateException("The rider already has an account");
        }
        this.accountStatus = RiderAccountStatus.INVITED;
    }

    // From an invite link (INVITED -> ACTIVE) or a reset link (ACTIVE stays ACTIVE). Never without a link.
    public void setPassword(String hash) {
        if (accountStatus == RiderAccountStatus.NONE) {
            throw new IllegalStateException("The rider was never invited");
        }
        this.passwordHash = hash;
        this.accountStatus = RiderAccountStatus.ACTIVE;
    }

    public void touch(Instant now) {
        this.lastActiveAt = now;
    }

    // First letters of the first and last name, e.g. "Sara Adel Hassan" -> "SH"; a single name gives one letter.
    public String initials() {
        if (name == null || name.isBlank()) {
            return "";
        }
        String[] parts = name.trim().split("\\s+");
        String first = firstLetter(parts[0]);
        if (parts.length == 1) {
            return first;
        }
        return first + firstLetter(parts[parts.length - 1]);
    }

    private static String firstLetter(String word) {
        return new String(Character.toChars(word.codePointAt(0))).toUpperCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Rider other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    // Constant so the hash does not change when the id is generated on save.
    @Override
    public int hashCode() {
        return Rider.class.hashCode();
    }
}
