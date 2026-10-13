package com.equestrian.equis.service.sessions;

import java.util.UUID;

// How many seats are taken in a template's sessions. Lets the template rules ask without knowing about bookings.
public interface BookedSeats {
    // The highest number of booked seats in any upcoming session of the template; 0 when there are none.
    int maxBookedInUpcoming(UUID templateId);

    // The number of bookings across all upcoming sessions of the template.
    int bookingsInUpcoming(UUID templateId);
}
