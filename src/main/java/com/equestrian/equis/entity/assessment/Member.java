package com.equestrian.equis.entity.assessment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

// A person the admin recorded to join the school, not placed in a level yet.
@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // Unique among all members, closed ones included.
    @Column(name = "phone", nullable = false, length = 11)
    private String phone;

    // Unique among all members and stored lowercase; the rider app will log in with it.
    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Set while the admin has closed the person; null otherwise. The other statuses are worked out (MemberStatus).
    @Column(name = "closed_at")
    private Instant closedAt;

    public static Member register(String name, String phone, String email) {
        Member member = new Member();
        member.setDetails(name, phone, email);
        return member;
    }

    // updated_at moves on the next flush (@UpdateTimestamp).
    public void updateDetails(String name, String phone, String email) {
        setDetails(name, phone, email);
    }

    public boolean isClosed() {
        return closedAt != null;
    }

    // Hides the person from the active lists; their bookings stay. Who may be closed is checked by the close rules.
    public void close(Instant now) {
        if (isClosed()) {
            throw new IllegalStateException("Member is already closed");
        }
        this.closedAt = now;
        this.updatedAt = now;
    }

    // The person comes back with the status of their last assessment.
    public void reopen(Instant now) {
        if (!isClosed()) {
            throw new IllegalStateException("Member is not closed");
        }
        this.closedAt = null;
        this.updatedAt = now;
    }

    // Trims all three and lowercases the email, so "Sara@Mail.com" and "sara@mail.com" are one address.
    private void setDetails(String name, String phone, String email) {
        this.name = name.trim();
        this.phone = phone.trim();
        this.email = email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Member other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    // Constant so the hash does not change when the id is generated on save.
    @Override
    public int hashCode() {
        return Member.class.hashCode();
    }
}
