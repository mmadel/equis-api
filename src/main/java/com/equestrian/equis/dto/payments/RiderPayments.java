package com.equestrian.equis.dto.payments;

import java.util.List;
import java.util.UUID;

// One rider's payment history on W3: today's balance and every package, newest first, voided ones included.
public record RiderPayments(UUID riderId, String riderName, BalanceDto balance, List<PaymentEntry> payments) {
}
