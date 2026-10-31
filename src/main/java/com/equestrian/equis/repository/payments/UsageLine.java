package com.equestrian.equis.repository.payments;

import com.equestrian.equis.dto.payments.PaymentUsage;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// One session used from a package, with the package it came from.
public record UsageLine(UUID paymentId, UUID sessionId, LocalDate date, LocalTime start, LocalTime end,
                        Short levelId, String trainerName, Instant usedAt) {

    public PaymentUsage toDto() {
        return new PaymentUsage(sessionId, date, start, end, levelId == null ? null : levelId.intValue(),
                trainerName, usedAt);
    }
}
