package com.equestrian.equis.service.reports.definitions;

import com.equestrian.equis.entity.reports.ExpiredPackageReportRow;
import com.equestrian.equis.repository.reports.ExpiredPackageReportRepository;
import com.equestrian.equis.service.reports.ReportColumn;
import com.equestrian.equis.service.reports.ReportDefinition;
import com.equestrian.equis.service.reports.ReportParams;
import com.equestrian.equis.service.reports.ReportTotal;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

// E19 · the packages whose last valid day falls from..to and is already over, with the sessions they lost. A package
// is usable until the end of its last day, so one that ends today is not listed yet. Fully used and voided packages
// are never listed. The total row adds up the sessions lost.
@Component
public class ExpiredSessionsReport implements ReportDefinition<ExpiredPackageReportRow> {
    static final String EXPIRED = "Expired";

    private static final List<ReportColumn<ExpiredPackageReportRow>> COLUMNS = List.of(
            ReportColumn.text("Rider", ExpiredPackageReportRow::getRiderName),
            ReportColumn.text("Trainer", ExpiredPackageReportRow::getTrainerName),
            ReportColumn.date("Package start", ExpiredPackageReportRow::getStartMonth),
            ReportColumn.integer("Months", ExpiredPackageReportRow::getMonths),
            ReportColumn.date("Expired on", ExpiredPackageReportRow::getValidUntil),
            ReportColumn.integer("Bought", ExpiredPackageReportRow::getSessions),
            ReportColumn.integer("Used", ExpiredPackageReportRow::getUsed),
            ReportColumn.integer(EXPIRED, ExpiredPackageReportRow::getExpired));

    private static final ReportTotal<ExpiredPackageReportRow> TOTAL =
            new ReportTotal<>("Total", row -> true, List.of(EXPIRED));

    private final ExpiredPackageReportRepository repository;

    public ExpiredSessionsReport(ExpiredPackageReportRepository repository) {
        this.repository = repository;
    }

    @Override
    public String name() {
        return "expired-sessions";
    }

    @Override
    public String title() {
        return "Expired sessions";
    }

    @Override
    public boolean dateRange() {
        return true;
    }

    @Override
    public List<ReportColumn<ExpiredPackageReportRow>> columns() {
        return COLUMNS;
    }

    @Override
    public Stream<ExpiredPackageReportRow> rows(ReportParams params) {
        return repository.streamExpired(params.from(), params.to(), today(params));
    }

    @Override
    public Optional<ReportTotal<ExpiredPackageReportRow>> total() {
        return Optional.of(TOTAL);
    }

    // The day the file was asked for, in the club's zone.
    static LocalDate today(ReportParams params) {
        return params.generatedAt().atZone(params.zone()).toLocalDate();
    }
}
