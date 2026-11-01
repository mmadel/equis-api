package com.equestrian.equis.dto.riderapp;

import java.util.List;

// The rider's payments, not voided, newest first.
public record RiderPayments(List<RiderPayment> payments) {
}
