package com.equestrian.equis.repository.assessment;

import java.util.UUID;

// The bookings of one assessment slot that aren't cancelled.
public interface SlotBookedCount {
    UUID getSessionId();

    long getBooked();
}
