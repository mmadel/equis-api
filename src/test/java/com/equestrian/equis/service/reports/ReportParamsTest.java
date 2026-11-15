package com.equestrian.equis.service.reports;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// E18 · export engine, case 10: the range's instants are whole days in the club's zone.
class ReportParamsTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");

    @Test
    void fromAndToAreWholeDaysInTheClubsZone() {
        ReportParams params = new ReportParams(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                LocalDate.of(2026, 11, 14), CAIRO, Instant.parse("2026-11-14T10:00:00Z"), Map.of());

        assertThat(params.fromInstant()).isEqualTo(Instant.parse("2026-10-31T22:00:00Z"));
        assertThat(params.toExclusive()).isEqualTo(Instant.parse("2026-11-30T22:00:00Z"));
    }

    @Test
    void filtersAreReadByKeyAndCopied() {
        Map<String, String> filters = new HashMap<>(Map.of("method", "cash"));
        ReportParams params = new ReportParams(null, null, LocalDate.of(2026, 11, 14), CAIRO,
                Instant.parse("2026-11-14T10:00:00Z"), filters);
        filters.put("method", "card");

        assertThat(params.filter("method")).contains("cash");
        assertThat(params.filter("other")).isEmpty();
    }
}
