package com.equestrian.equis.service.reports.definitions;

import com.equestrian.equis.entity.payments.PaymentMethod;
import com.equestrian.equis.entity.reports.PaymentReportRow;
import com.equestrian.equis.repository.reports.PaymentReportRepository;
import com.equestrian.equis.service.reports.ReportColumn;
import com.equestrian.equis.service.reports.ReportDefinition;
import com.equestrian.equis.service.reports.ReportParams;
import com.equestrian.equis.service.reports.ReportTotal;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

// E19 · the payments recorded from..to (by recorded_at, whole days in the club's zone), voided ones included and
// marked with their reason. The total row adds up the valid payments only.
@Component
public class PaymentsReport implements ReportDefinition<PaymentReportRow> {
    static final String AMOUNT = "Amount";
    static final String SESSIONS = "Sessions";

    private static final List<ReportColumn<PaymentReportRow>> COLUMNS = List.of(
            ReportColumn.dateTime("Recorded at", PaymentReportRow::getRecordedAt),
            ReportColumn.text("Rider", PaymentReportRow::getRiderName),
            ReportColumn.text("Trainer", PaymentReportRow::getTrainerName),
            ReportColumn.money(AMOUNT, PaymentReportRow::getAmountEgp),
            ReportColumn.text("Method", row -> methodLabel(row.getMethod())),
            ReportColumn.date("Start month", PaymentReportRow::getStartMonth),
            ReportColumn.integer("Months", PaymentReportRow::getMonths),
            ReportColumn.integer(SESSIONS, PaymentReportRow::getSessions),
            ReportColumn.text("Status", row -> row.isVoided() ? "Voided" : "Valid"),
            ReportColumn.text("Void reason", PaymentReportRow::getVoidReason),
            ReportColumn.text("Recorded by", PaymentReportRow::getRecordedByName));

    private static final ReportTotal<PaymentReportRow> TOTAL =
            new ReportTotal<>("Total (valid)", row -> !row.isVoided(), List.of(AMOUNT, SESSIONS));

    private final PaymentReportRepository repository;

    public PaymentsReport(PaymentReportRepository repository) {
        this.repository = repository;
    }

    @Override
    public String name() {
        return "payments";
    }

    @Override
    public String title() {
        return "Payments";
    }

    @Override
    public boolean dateRange() {
        return true;
    }

    @Override
    public List<ReportColumn<PaymentReportRow>> columns() {
        return COLUMNS;
    }

    @Override
    public Stream<PaymentReportRow> rows(ReportParams params) {
        return repository.streamRecorded(params.fromInstant(), params.toExclusive());
    }

    @Override
    public Optional<ReportTotal<PaymentReportRow>> total() {
        return Optional.of(TOTAL);
    }

    static String methodLabel(PaymentMethod method) {
        if (method == null) {
            return null;
        }
        return switch (method) {
            case CASH -> "Cash";
            case CARD -> "Card";
            case BANK_TRANSFER -> "Bank transfer";
            case MOBILE_WALLET -> "Mobile wallet";
            case OTHER -> "Other";
        };
    }
}
