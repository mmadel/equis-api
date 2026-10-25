package com.equestrian.equis.dto.payments;

import com.equestrian.equis.entity.payments.RiderPaymentStatus;
import com.equestrian.equis.service.payments.RiderBalance;

import java.time.LocalDate;

// A rider's balance as every payments endpoint sends it. nextExpiry is null when nothing is left.
public record BalanceDto(int sessionsLeft, RiderPaymentStatus status, LocalDate nextExpiry) {
    public static BalanceDto of(RiderBalance b) {
        return new BalanceDto(b.sessionsLeft(), b.status(), b.nextExpiry());
    }
}
