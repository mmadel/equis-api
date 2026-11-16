package com.equestrian.equis.service.reports.definitions;

import com.equestrian.equis.entity.payments.RiderPaymentStatus;

import java.time.LocalDate;

// One row of the Rider balances report: a rider's balance today. bought and used cover the packages valid today;
// nextExpiry is null when nothing is left.
public record RiderBalanceReportRow(String riderName, String trainerName, RiderPaymentStatus status,
                                    int bought, int used, int remaining, LocalDate nextExpiry) {
}
