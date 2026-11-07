package com.equestrian.equis.service.accounts;

import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.SubjectType;

import java.time.Instant;
import java.util.UUID;

// Published inside the transaction that made the link; the email is sent after commit.
// subjectType is TRAINER, RIDER or ADMIN: it picks the app the link opens and the email's wording.
// clubName and clubCode are set only for ADMIN links (the email names the club and gives its code).
// The raw token lives only in this in-memory event.
public record AccountLinkCreatedEvent(SubjectType subjectType, UUID subjectId, String name, String email,
                                      InvitePurpose purpose, String rawToken, Instant expiresAt,
                                      String clubName, String clubCode) {

    // A trainer or rider link: the email doesn't mention the club.
    public AccountLinkCreatedEvent(SubjectType subjectType, UUID subjectId, String name, String email,
                                   InvitePurpose purpose, String rawToken, Instant expiresAt) {
        this(subjectType, subjectId, name, email, purpose, rawToken, expiresAt, null, null);
    }

    // Never print the token.
    @Override
    public String toString() {
        return "AccountLinkCreatedEvent[subjectType=" + subjectType + ", subjectId=" + subjectId
                + ", purpose=" + purpose + "]";
    }
}
