package com.equestrian.equis.service.sessions;

// What deleting a template did: how many upcoming sessions were cancelled, and how many bookings they held.
public record DeleteImpact(int cancelledSessions, int cancelledBookings) {
}
