package com.equestrian.equis.dto.platform;

import com.equestrian.equis.entity.platform.AccountActionType;

import java.time.Instant;
import java.util.UUID;

// One row of a club's Activity tab: what the super admin did to which account, and when. accountName and
// accountEmail are the copies taken at that moment. doneByName is the super admin's email: super_admin has no name.
public record AccountActivityRow(UUID id, Instant doneAt, AccountType accountType, UUID accountId,
                                 String accountName, String accountEmail, AccountActionType action,
                                 String doneByName) {
}
