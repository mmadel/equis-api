package com.equestrian.equis.dto.riderapp;

import java.math.BigDecimal;
import java.time.YearMonth;

// The rider's average stars in one month (one decimal) and how many evaluations it is over.
public record StarTrendPoint(YearMonth month, BigDecimal averageStars, long evaluations) {
}
