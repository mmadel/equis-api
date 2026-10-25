package com.equestrian.equis.service.payments;

import com.equestrian.equis.entity.payments.RiderPaymentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// A rider's sessions left on a date. nextExpiry is when the next sessions expire, null when none are left.
// packages are the valid ones, in the order sessions come out of them.
public record RiderBalance(UUID riderId, int sessionsLeft, RiderPaymentStatus status, LocalDate nextExpiry,
                           List<PackageBalance> packages) {

    public static RiderBalance empty(UUID riderId) {
        return new RiderBalance(riderId, 0, RiderPaymentStatus.AWAITING_PAYMENT, null, List.of());
    }
}
