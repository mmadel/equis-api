package com.equestrian.equis.service.reports.definitions;

import com.equestrian.equis.entity.reports.TrainerReportRow;
import com.equestrian.equis.repository.reports.TrainerReportRepository;
import com.equestrian.equis.service.reports.ReportColumn;
import com.equestrian.equis.service.reports.ReportDefinition;
import com.equestrian.equis.service.reports.ReportParams;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

// The club's trainers as of today: Name, Email, Phone, Status.
@Component
public class TrainersReport implements ReportDefinition<TrainerReportRow> {
    private static final List<ReportColumn<TrainerReportRow>> COLUMNS = List.of(
            ReportColumn.text("Name", TrainerReportRow::getName),
            ReportColumn.text("Email", TrainerReportRow::getEmail),
            ReportColumn.text("Phone", TrainerReportRow::getPhone),
            ReportColumn.text("Status", row -> label(row.getStatus())));

    private final TrainerReportRepository repository;

    public TrainersReport(TrainerReportRepository repository) {
        this.repository = repository;
    }

    @Override
    public String name() {
        return "trainers";
    }

    @Override
    public String title() {
        return "Trainers";
    }

    @Override
    public boolean dateRange() {
        return false;
    }

    @Override
    public List<ReportColumn<TrainerReportRow>> columns() {
        return COLUMNS;
    }

    @Override
    public Stream<TrainerReportRow> rows(ReportParams params) {
        return repository.streamAll();
    }

    // "active" -> "Active".
    static String label(String status) {
        if (status == null || status.isEmpty()) {
            return status;
        }
        return status.substring(0, 1).toUpperCase(Locale.ROOT) + status.substring(1);
    }
}
