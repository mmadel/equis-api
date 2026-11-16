package com.equestrian.equis.service.reports.definitions;

import com.equestrian.equis.entity.payments.RiderPaymentStatus;
import com.equestrian.equis.entity.reports.RiderReportRow;
import com.equestrian.equis.repository.reports.RiderReportRepository;
import com.equestrian.equis.service.payments.PackageBalance;
import com.equestrian.equis.service.payments.RiderBalance;
import com.equestrian.equis.service.payments.RiderBalanceService;
import com.equestrian.equis.service.reports.Chunks;
import com.equestrian.equis.service.reports.ReportColumn;
import com.equestrian.equis.service.reports.ReportDefinition;
import com.equestrian.equis.service.reports.ReportParams;
import com.equestrian.equis.service.reports.ReportTotal;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

// E19 · every rider of the club with today's balance, zero balances included. The riders come from the view
// report_rider_v; the balances from RiderBalanceService, the same rules as the Riders screen and the apps (only
// packages valid today, oldest package first, voided payments ignored). The total row adds up the whole club:
// its Remaining is the number of sessions the club still owes.
@Component
public class RiderBalancesReport implements ReportDefinition<RiderBalanceReportRow> {
    static final int BATCH = 500;
    static final String BOUGHT = "Bought";
    static final String USED = "Used";
    static final String REMAINING = "Remaining";

    private static final List<ReportColumn<RiderBalanceReportRow>> COLUMNS = List.of(
            ReportColumn.text("Rider", RiderBalanceReportRow::riderName),
            ReportColumn.text("Trainer", RiderBalanceReportRow::trainerName),
            ReportColumn.text("Status", row -> statusLabel(row.status())),
            ReportColumn.integer(BOUGHT, RiderBalanceReportRow::bought),
            ReportColumn.integer(USED, RiderBalanceReportRow::used),
            ReportColumn.integer(REMAINING, RiderBalanceReportRow::remaining),
            ReportColumn.date("Next expiry", RiderBalanceReportRow::nextExpiry));

    private static final ReportTotal<RiderBalanceReportRow> TOTAL = ReportTotal.of("Total", BOUGHT, USED, REMAINING);

    private final RiderReportRepository repository;
    private final RiderBalanceService balanceService;

    public RiderBalancesReport(RiderReportRepository repository, RiderBalanceService balanceService) {
        this.repository = repository;
        this.balanceService = balanceService;
    }

    @Override
    public String name() {
        return "rider-balances";
    }

    @Override
    public String title() {
        return "Rider balances";
    }

    @Override
    public boolean dateRange() {
        return false;
    }

    @Override
    public List<ReportColumn<RiderBalanceReportRow>> columns() {
        return COLUMNS;
    }

    // BATCH riders at a time: two queries per batch, and a large club is never loaded at once.
    @Override
    public Stream<RiderBalanceReportRow> rows(ReportParams params) {
        LocalDate asOf = params.asOf();
        return Chunks.of(repository.streamAll(), BATCH)
                .flatMap(riders -> {
                    List<UUID> ids = riders.stream().map(RiderReportRow::getId).toList();
                    Map<UUID, RiderBalance> balances = balanceService.balancesOn(ids, asOf);
                    return riders.stream().map(rider -> row(rider, balances.get(rider.getId())));
                });
    }

    @Override
    public Optional<ReportTotal<RiderBalanceReportRow>> total() {
        return Optional.of(TOTAL);
    }

    static RiderBalanceReportRow row(RiderReportRow rider, RiderBalance balance) {
        RiderBalance b = balance != null ? balance : RiderBalance.empty(rider.getId());
        int bought = b.packages().stream().mapToInt(PackageBalance::sessions).sum();
        int used = b.packages().stream().mapToInt(PackageBalance::used).sum();
        return new RiderBalanceReportRow(rider.getRiderName(), rider.getTrainerName(), b.status(),
                bought, used, b.sessionsLeft(), b.nextExpiry());
    }

    static String statusLabel(RiderPaymentStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case ACTIVE -> "Active";
            case AWAITING_PAYMENT -> "Awaiting payment";
        };
    }
}
