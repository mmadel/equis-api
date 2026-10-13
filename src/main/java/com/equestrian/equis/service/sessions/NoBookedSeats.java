package com.equestrian.equis.service.sessions;

import org.springframework.stereotype.Component;

import java.util.UUID;

// Stands in until bookings exist: nothing is ever booked. Replace with the real count when they do.
@Component
public class NoBookedSeats implements BookedSeats {
    @Override
    public int maxBookedInUpcoming(UUID templateId) {
        return 0;
    }

    @Override
    public int bookingsInUpcoming(UUID templateId) {
        return 0;
    }
}
