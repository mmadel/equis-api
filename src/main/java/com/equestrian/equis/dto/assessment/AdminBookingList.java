package com.equestrian.equis.dto.assessment;

import java.util.List;

public record AdminBookingList(BookingTotals totals, List<AdminBookingRow> bookings) {
}
