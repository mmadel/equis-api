package com.equestrian.equis.dto.riderapp;

import java.time.LocalDate;

// Sessions left in packages that expire within 7 days, and the earliest of those expiry dates.
public record ExpiringSoon(int sessions, LocalDate date) {
}
