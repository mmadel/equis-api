package com.equestrian.equis.service.accounts;

import com.equestrian.equis.entity.accounts.InvitePurpose;

import java.time.Instant;
import java.util.UUID;

// Published inside the transaction that made the link; the email is sent after commit.
// The raw token lives only in this in-memory event.
public record AccountLinkCreatedEvent(UUID trainerId, String trainerName, String email,
                                      InvitePurpose purpose, String rawToken, Instant expiresAt) {
    // Never print the token.
    @Override
    public String toString() {
        return "AccountLinkCreatedEvent[trainerId=" + trainerId + ", purpose=" + purpose + "]";
    }
}
