package com.equestrian.equis.dto.assessment;

// The numbers at the top of W2. Always over every member's latest booking, whatever the filter.
public record BookingTotals(long upcoming, long waiting, long placed, long noshow, long cancelled) {
}
