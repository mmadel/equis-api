package com.equestrian.equis.dto.riderapp;

import com.equestrian.equis.entity.payments.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

// One payment as the rider sees it. recordedOn is the date in the school time zone.
public record RiderPayment(UUID paymentId, LocalDate recordedOn, BigDecimal amountEgp, PaymentMethod method,
                           YearMonth startMonth, int months, int sessions, LocalDate validUntil) {
}
