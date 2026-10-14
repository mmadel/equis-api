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
import java.util.UUID;

// A walk-in registered by a trainer, not placed in a level yet.
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

    // Unique when present; several members may have none.
    @Column(name = "phone", length = 11)
    private String phone;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // A blank phone is stored as null so it never clashes on the unique key.
    public static Member register(String name, String phone) {
        Member member = new Member();
        member.name = name.trim();
        member.phone = phone == null || phone.isBlank() ? null : phone.trim();
        return member;
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
