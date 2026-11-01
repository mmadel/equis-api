package com.equestrian.equis.service.accounts;

import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.SubjectType;

import java.time.Instant;
import java.util.UUID;

// Published inside the transaction that made the link; the email is sent after commit.
// subjectType is TRAINER or RIDER: it picks the app the link opens and the email's wording.
// The raw token lives only in this in-memory event.
public record AccountLinkCreatedEvent(SubjectType subjectType, UUID subjectId, String name, String email,
                                      InvitePurpose purpose, String rawToken, Instant expiresAt) {
    // Never print the token.
    @Override
    public String toString() {
        return "AccountLinkCreatedEvent[subjectType=" + subjectType + ", subjectId=" + subjectId
                + ", purpose=" + purpose + "]";
    }
}
