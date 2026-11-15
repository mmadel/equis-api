package com.equestrian.equis.service.reports;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

// What a report is asked for. clubName is the current club's name, for the file's title block. from and to are
// whole days in the club's zone, both included; they are null for a snapshot report. asOf is today in that zone,
// generatedAt the moment the file was asked for. filters are the other query parameters, for reports that have
// their own filters.
public record ReportParams(String clubName, LocalDate from, LocalDate to, LocalDate asOf, ZoneId zone,
                           Instant generatedAt, Map<String, String> filters) {

    public ReportParams {
        filters = Map.copyOf(filters);
    }

    // The first moment of from, in the club's zone.
    public Instant fromInstant() {
        return from.atStartOfDay(zone).toInstant();
    }

    // The first moment of the day after to, in the club's zone: a row is in the range when it is before this.
    public Instant toExclusive() {
        return to.plusDays(1).atStartOfDay(zone).toInstant();
    }

    public Optional<String> filter(String key) {
        return Optional.ofNullable(filters.get(key));
    }
}
