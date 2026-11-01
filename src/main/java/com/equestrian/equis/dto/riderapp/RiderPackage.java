package com.equestrian.equis.dto.riderapp;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

// A package that counts today: what it holds, what was used, what is left.
public record RiderPackage(UUID paymentId, YearMonth startMonth, LocalDate validUntil, int sessions, int used,
                           int left) {
}
