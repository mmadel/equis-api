package com.equestrian.equis.service.payments;

import java.time.LocalDate;
import java.util.UUID;

// One valid package on a date: what it holds, what was used, what is left.
public record PackageBalance(UUID paymentId, LocalDate startMonth, LocalDate validUntil,
                             int sessions, int used, int left) {
}
