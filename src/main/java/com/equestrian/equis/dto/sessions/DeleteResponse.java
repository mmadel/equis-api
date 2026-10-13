package com.equestrian.equis.dto.sessions;

// How many upcoming sessions the delete cancelled, and how many bookings they held.
public record DeleteResponse(int cancelledSessions, int cancelledBookings) {
}
