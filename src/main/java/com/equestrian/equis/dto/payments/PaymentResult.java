package com.equestrian.equis.dto.payments;

// The saved package and the rider's balance after it, for today.
public record PaymentResult(PaymentEntry payment, BalanceDto balance) {
}
