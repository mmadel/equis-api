package com.equestrian.equis.service.reports;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

// Test-only range report "all-types": one column of each type, and a "Total (valid)" row over Amount and Count that
// skips voided rows. The writer tests pass the rows themselves, so rows() is never called. Not a bean.
public class AllTypesReport implements ReportDefinition<AllTypesReport.Line> {
    public static final String TOTAL_LABEL = "Total (valid)";

    // day may be anything, to try a wrong Java type in the DATE column.
    public record Line(String name, Object day, Instant at, BigDecimal amount, Integer count, boolean voided) {

        public static Line of(String name, BigDecimal amount, Integer count, boolean voided) {
            return new Line(name, null, null, amount, count, voided);
        }
    }

    private final boolean withTotal;

    public AllTypesReport(boolean withTotal) {
        this.withTotal = withTotal;
    }

    @Override
    public String name() {
        return "all-types";
    }

    @Override
    public String title() {
        return "All types";
    }

    @Override
    public boolean dateRange() {
        return true;
    }

    @Override
    public List<ReportColumn<Line>> columns() {
        return List.of(
                ReportColumn.text("Name", Line::name),
                ReportColumn.date("Day", Line::day),
                ReportColumn.dateTime("At", Line::at),
                ReportColumn.money("Amount", Line::amount),
                ReportColumn.integer("Count", Line::count));
    }

    @Override
    public Stream<Line> rows(ReportParams params) {
        throw new UnsupportedOperationException("the writer tests pass the rows");
    }

    @Override
    public Optional<ReportTotal<Line>> total() {
        return withTotal
                ? Optional.of(new ReportTotal<>(TOTAL_LABEL, line -> !line.voided(), List.of("Amount", "Count")))
                : Optional.empty();
    }
}
