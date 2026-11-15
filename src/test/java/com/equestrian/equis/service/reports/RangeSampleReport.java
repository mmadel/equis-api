package com.equestrian.equis.service.reports;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

// Test-only range report "test-range": one row per day from..to. A plain @Component in the test sources, so every
// full-app test context has it and ReportExportIT shares the common context (no @TestConfiguration of its own).
@Component
public class RangeSampleReport implements ReportDefinition<LocalDate> {
    public static final String NAME = "test-range";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String title() {
        return "Test range";
    }

    @Override
    public boolean dateRange() {
        return true;
    }

    @Override
    public List<ReportColumn<LocalDate>> columns() {
        return List.of(ReportColumn.date("Day", d -> d), ReportColumn.integer("Day of month", LocalDate::getDayOfMonth));
    }

    @Override
    public Stream<LocalDate> rows(ReportParams params) {
        return params.from().datesUntil(params.to().plusDays(1));
    }
}
