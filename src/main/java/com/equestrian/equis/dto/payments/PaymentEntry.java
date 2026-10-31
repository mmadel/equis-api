package com.equestrian.equis.dto.payments;

import com.equestrian.equis.entity.payments.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

// One payment package as every payments endpoint sends it. state is a PaymentState code on the day asked for;
// left is 0 unless the package is upcoming or active. voidedAt and voidReason are null until voided.
// usages are the sessions taken from the package, oldest first; used is always usages.size().
public record PaymentEntry(UUID id, UUID riderId, BigDecimal amountEgp, PaymentMethod method, YearMonth startMonth,
                           int months, int sessions, LocalDate validUntil, int used, int left, String state,
                           Instant recordedAt, Instant voidedAt, String voidReason, List<PaymentUsage> usages) {
}
