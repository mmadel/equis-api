package com.equestrian.equis.service.sessions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Upcoming sessions of a template were cancelled by a template delete or edit.
// Published inside the transaction that cancelled them.
public record SessionsCancelledEvent(UUID templateId, List<UUID> sessionIds, Instant cancelledAt) {
    public SessionsCancelledEvent {
        sessionIds = List.copyOf(sessionIds);
    }
}
