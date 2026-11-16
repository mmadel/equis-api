package com.equestrian.equis.service.reports.definitions;

import com.equestrian.equis.repository.reports.ExpiredPackageReportRepository;
import com.equestrian.equis.service.reports.ColumnType;
import com.equestrian.equis.service.reports.ReportColumn;
import com.equestrian.equis.service.reports.ReportParams;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpiredSessionsReportTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");

    private final ExpiredPackageReportRepository repository = mock(ExpiredPackageReportRepository.class);
    private final ExpiredSessionsReport report = new ExpiredSessionsReport(repository);

    @Test
    void isTheRangeReportNamedExpiredSessions() {
        assertThat(report.name()).isEqualTo("expired-sessions");
        assertThat(report.title()).isEqualTo("Expired sessions");
        assertThat(report.dateRange()).isTrue();
    }

    @Test
    void hasTheColumnsOfTheTicketInOrder() {
        assertThat(report.columns()).extracting(ReportColumn::header).containsExactly("Rider", "Trainer",
                "Package start", "Months", "Expired on", "Bought", "Used", "Expired");
        assertThat(report.columns()).extracting(ReportColumn::type).containsExactly(ColumnType.TEXT,
                ColumnType.TEXT, ColumnType.DATE, ColumnType.INTEGER, ColumnType.DATE, ColumnType.INTEGER,
                ColumnType.INTEGER, ColumnType.INTEGER);
    }

    @Test
    void totalsTheExpiredSessionsOfEveryRow() {
        assertThat(report.total()).hasValueSatisfying(total -> {
            assertThat(total.label()).isEqualTo("Total");
            assertThat(total.sumHeaders()).containsExactly("Expired");
        });
    }

    // Just after midnight in Cairo is still the previous day in UTC: today is the Cairo day.
    @Test
    void readsTheRangeWithTodayInTheClubsZone() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 30);
        ReportParams params = new ReportParams("School", from, to, LocalDate.of(2026, 12, 1), CAIRO,
                ZonedDateTime.of(2026, 12, 1, 0, 30, 0, 0, CAIRO).toInstant(), Map.of());
        when(repository.streamExpired(from, to, LocalDate.of(2026, 12, 1))).thenReturn(Stream.empty());

        assertThat(report.rows(params)).isEmpty();
        verify(repository).streamExpired(from, to, LocalDate.of(2026, 12, 1));
    }
}
