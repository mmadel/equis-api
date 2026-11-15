package com.equestrian.equis.service.reports;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// E18 · export engine: the date rules (decisions 5, 7, 8, 9).
class ReportParamsFactoryTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // 14 Nov 2026 00:30 in Cairo is still 13 Nov in UTC: "today" must be Cairo's.
    private static final Instant NOW = ZonedDateTime.of(2026, 11, 14, 0, 30, 0, 0, CAIRO).toInstant();

    private final ReportParamsFactory factory = new ReportParamsFactory(Clock.fixed(NOW, CAIRO), CAIRO);
    private final ReportDefinition<LocalDate> range = new RangeSampleReport();
    private final ReportDefinition<String> snapshot = new Snapshot();

    @Test
    void aSnapshotIsAsOfTodayInTheClubsZoneAndIgnoresTheDates() {
        ReportParams params = factory.build(snapshot, Map.of("from", "nonsense", "to", "2026-01-01", "foo", "bar"));

        assertThat(params.from()).isNull();
        assertThat(params.to()).isNull();
        assertThat(params.asOf()).isEqualTo(LocalDate.of(2026, 11, 14));
        assertThat(params.generatedAt()).isEqualTo(NOW);
        assertThat(params.zone()).isEqualTo(CAIRO);
        assertThat(params.filters()).containsExactly(Map.entry("foo", "bar"));
    }

    @Test
    void aRangeReportReadsBothDates() {
        ReportParams params = factory.build(range, Map.of("from", "2026-01-01", "to", "2026-12-31", "method", "cash"));

        assertThat(params.from()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(params.to()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(params.asOf()).isEqualTo(LocalDate.of(2026, 11, 14));
        assertThat(params.filter("method")).contains("cash");
        assertThat(params.filters()).doesNotContainKeys("from", "to");
    }

    @Test
    void oneDayIsAValidRange() {
        ReportParams params = factory.build(range, Map.of("from", "2026-11-14", "to", "2026-11-14"));

        assertThat(params.from()).isEqualTo(params.to());
    }

    @Test
    void missingDatesAreFieldErrors() {
        assertThat(fieldErrors(Map.of())).extracting(ApiFieldError::field).containsExactly("from", "to");
        assertThat(fieldErrors(Map.of("to", "2026-11-30"))).extracting(ApiFieldError::field).containsExactly("from");
    }

    @Test
    void aBadlyFormattedDateIsAFieldError() {
        assertThat(fieldErrors(Map.of("from", "2026-13-01", "to", "2026-11-30")))
                .containsExactly(new ApiFieldError("from", ErrorCodes.INVALID_DATE));
    }

    @Test
    void fromAfterToIsAFieldErrorOnTo() {
        assertThat(fieldErrors(Map.of("from", "2026-11-30", "to", "2026-11-01")))
                .containsExactly(new ApiFieldError("to", ErrorCodes.INVALID_RANGE));
    }

    @Test
    void twelveMonthsOrMoreIsTooLong() {
        assertThatThrownBy(() -> factory.build(range, Map.of("from", "2026-01-01", "to", "2027-01-01")))
                .isInstanceOfSatisfying(ReportRangeTooLongException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(400);
                    assertThat(e.code()).isEqualTo(ErrorCodes.REPORT_RANGE_TOO_LONG);
                    assertThat(e.properties()).containsEntry("maxMonths", 12);
                });
    }

    @SuppressWarnings("unchecked")
    private List<ApiFieldError> fieldErrors(Map<String, String> query) {
        try {
            factory.build(range, query);
        } catch (ApiException e) {
            assertThat(e.status().value()).isEqualTo(400);
            assertThat(e.code()).isEqualTo(ErrorCodes.VALIDATION_FAILED);
            return (List<ApiFieldError>) e.properties().get("fieldErrors");
        }
        throw new AssertionError("expected a validation error for " + query);
    }

    private static final class Snapshot implements ReportDefinition<String> {
        @Override
        public String name() {
            return "snapshot";
        }

        @Override
        public String title() {
            return "Snapshot";
        }

        @Override
        public boolean dateRange() {
            return false;
        }

        @Override
        public List<ReportColumn<String>> columns() {
            return List.of();
        }

        @Override
        public Stream<String> rows(ReportParams params) {
            return Stream.empty();
        }
    }
}
