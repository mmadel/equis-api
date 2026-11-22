package com.equestrian.equis.entity.platform;

import com.equestrian.equis.dto.platform.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// One thing the super admin did to a club's account. Global, like subscription: no @TenantId, no RLS; clubId is a
// plain foreign key. Never edited. The account's name and email are copies taken at that moment.
@Entity
@Table(name = "platform_account_action")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlatformAccountAction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "club_id", nullable = false, updatable = false)
    private UUID clubId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 10, updatable = false)
    private AccountType accountType;

    // An admin_account, trainer or rider id, by accountType.
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "account_name", nullable = false, length = 120, updatable = false)
    private String accountName;

    @Column(name = "account_email", nullable = false, length = 254, updatable = false)
    private String accountEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30, updatable = false)
    private AccountActionType action;

    // The super admin's id.
    @Column(name = "done_by", nullable = false, updatable = false)
    private UUID doneBy;

    @Column(name = "done_at", nullable = false, updatable = false)
    private Instant doneAt;

    public static PlatformAccountAction record(UUID clubId, AccountType accountType, UUID accountId,
                                               String accountName, String accountEmail, AccountActionType action,
                                               UUID doneBy, Instant doneAt) {
        PlatformAccountAction row = new PlatformAccountAction();
        row.clubId = Objects.requireNonNull(clubId, "clubId");
        row.accountType = Objects.requireNonNull(accountType, "accountType");
        row.accountId = Objects.requireNonNull(accountId, "accountId");
        row.accountName = Objects.requireNonNull(accountName, "accountName");
        row.accountEmail = Objects.requireNonNull(accountEmail, "accountEmail");
        row.action = Objects.requireNonNull(action, "action");
        row.doneBy = Objects.requireNonNull(doneBy, "doneBy");
        row.doneAt = Objects.requireNonNull(doneAt, "doneAt");
        return row;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlatformAccountAction other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    // Constant so the hash does not change when the id is generated on save.
    @Override
    public int hashCode() {
        return PlatformAccountAction.class.hashCode();
    }
}
