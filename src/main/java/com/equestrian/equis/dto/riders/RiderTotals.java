package com.equestrian.equis.dto.riders;

// The pills at the top of W3. Always over every rider, whatever the filter.
public record RiderTotals(long total, long fromAssessment, long changedManually, long setManually, long unassigned) {
}
