package com.equestrian.equis.service.reports.definitions;

import com.equestrian.equis.entity.payments.RiderPaymentStatus;
import com.equestrian.equis.entity.reports.RiderReportRow;
import com.equestrian.equis.repository.reports.RiderReportRepository;
import com.equestrian.equis.service.payments.PackageBalance;
import com.equestrian.equis.service.payments.RiderBalance;
import com.equestrian.equis.service.payments.RiderBalanceService;
import com.equestrian.equis.service.reports.ColumnType;
import com.equestrian.equis.service.reports.ReportColumn;
import com.equestrian.equis.service.reports.ReportParams;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiderBalancesReportTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);
    private static final ReportParams PARAMS = new ReportParams("Riding School", null, null, TODAY,
            ZoneId.of("Africa/Cairo"), Instant.parse("2026-11-16T18:30:00Z"), Map.of());

    private final RiderReportRepository repository = mock(RiderReportRepository.class);
    private final RiderBalanceService balances = mock(RiderBalanceService.class);
    private final RiderBalancesReport report = new RiderBalancesReport(repository, balances);

    @Test
    void isTheSnapshotReportNamedRiderBalances() {
        assertThat(report.name()).isEqualTo("rider-balances");
        assertThat(report.title()).isEqualTo("Rider balances");
        assertThat(report.dateRange()).isFalse();
    }

    @Test
    void hasTheColumnsOfTheTicketInOrder() {
        assertThat(report.columns()).extracting(ReportColumn::header).containsExactly("Rider", "Trainer", "Status",
                "Bought", "Used", "Remaining", "Next expiry");
        assertThat(report.columns()).extracting(ReportColumn::type).containsExactly(ColumnType.TEXT, ColumnType.TEXT,
                ColumnType.TEXT, ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.DATE);
    }

    @Test
    void totalsBoughtUsedAndRemainingOverEveryRider() {
        assertThat(report.total()).hasValueSatisfying(total -> {
            assertThat(total.label()).isEqualTo("Total");
            assertThat(total.sumHeaders()).containsExactly("Bought", "Used", "Remaining");
            assertThat(total.include().test(row("Ziad", RiderPaymentStatus.AWAITING_PAYMENT))).isTrue();
        });
    }

    @Test
    void labelsEveryStatus() {
        assertThat(RiderBalancesReport.statusLabel(RiderPaymentStatus.ACTIVE)).isEqualTo("Active");
        assertThat(RiderBalancesReport.statusLabel(RiderPaymentStatus.AWAITING_PAYMENT)).isEqualTo("Awaiting payment");
        assertThat(RiderBalancesReport.statusLabel(null)).isNull();
    }

    @Test
    void addsUpThePackagesValidToday() {
        RiderReportRow laila = rider("Laila", "Trainer One");
        RiderBalance balance = new RiderBalance(laila.getId(), 11, RiderPaymentStatus.ACTIVE,
                LocalDate.of(2026, 11, 30), List.of(
                        new PackageBalance(UUID.randomUUID(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30), 8, 7, 1),
                        new PackageBalance(UUID.randomUUID(), LocalDate.of(2026, 11, 1), LocalDate.of(2027, 1, 31), 16, 6, 10)));

        RiderBalanceReportRow row = RiderBalancesReport.row(laila, balance);

        assertThat(row).isEqualTo(new RiderBalanceReportRow("Laila", "Trainer One", RiderPaymentStatus.ACTIVE,
                24, 13, 11, LocalDate.of(2026, 11, 30)));
    }

    @Test
    void aRiderWithoutABalanceGetsZerosAndAwaitingPayment() {
        RiderBalanceReportRow row = RiderBalancesReport.row(rider("Ziad", null), null);

        assertThat(row).isEqualTo(new RiderBalanceReportRow("Ziad", null, RiderPaymentStatus.AWAITING_PAYMENT,
                0, 0, 0, null));
    }

    // E19 · Rider balances, case 10: 1,200 riders, in the repository's order, three balancesOn calls.
    @Test
    void readsTheBalancesInBatchesOf500() {
        List<RiderReportRow> riders = IntStream.range(0, 1200)
                .mapToObj(i -> rider("Rider %04d".formatted(i), "Trainer One")).toList();
        AtomicBoolean closed = new AtomicBoolean();
        when(repository.streamAll()).thenReturn(riders.stream().onClose(() -> closed.set(true)));
        when(balances.balancesOn(anyCollection(), eq(TODAY))).thenAnswer(call -> {
            Collection<UUID> ids = call.getArgument(0);
            Map<UUID, RiderBalance> result = new LinkedHashMap<>();
            ids.forEach(id -> result.put(id, RiderBalance.empty(id)));
            return result;
        });

        List<RiderBalanceReportRow> rows;
        try (Stream<RiderBalanceReportRow> stream = report.rows(PARAMS)) {
            rows = stream.toList();
        }

        assertThat(rows).hasSize(1200);
        assertThat(rows).extracting(RiderBalanceReportRow::riderName)
                .containsExactlyElementsOf(riders.stream().map(RiderReportRow::getRiderName).toList());
        verify(balances, times(3)).balancesOn(any(), eq(TODAY));
        assertThat(closed).isTrue();
    }

    private static RiderReportRow rider(String name, String trainer) {
        RiderReportRow row = mock(RiderReportRow.class);
        UUID id = UUID.randomUUID();
        when(row.getId()).thenReturn(id);
        when(row.getRiderName()).thenReturn(name);
        when(row.getTrainerName()).thenReturn(trainer);
        return row;
    }

    private static RiderBalanceReportRow row(String name, RiderPaymentStatus status) {
        return new RiderBalanceReportRow(name, null, status, 0, 0, 0, null);
    }
}
