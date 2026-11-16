package com.equestrian.equis.service.reports.definitions;

import com.equestrian.equis.entity.payments.PaymentMethod;
import com.equestrian.equis.repository.reports.PaymentReportRepository;
import com.equestrian.equis.service.reports.ColumnType;
import com.equestrian.equis.service.reports.ReportColumn;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PaymentsReportTest {
    private final PaymentsReport report = new PaymentsReport(mock(PaymentReportRepository.class));

    @Test
    void isTheRangeReportNamedPayments() {
        assertThat(report.name()).isEqualTo("payments");
        assertThat(report.title()).isEqualTo("Payments");
        assertThat(report.dateRange()).isTrue();
    }

    @Test
    void hasTheColumnsOfTheTicketInOrder() {
        assertThat(report.columns()).extracting(ReportColumn::header).containsExactly("Recorded at", "Rider",
                "Trainer", "Amount", "Method", "Start month", "Months", "Sessions", "Status", "Void reason",
                "Recorded by");
        assertThat(report.columns()).extracting(ReportColumn::type).containsExactly(ColumnType.DATETIME,
                ColumnType.TEXT, ColumnType.TEXT, ColumnType.MONEY_EGP, ColumnType.TEXT, ColumnType.DATE,
                ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.TEXT, ColumnType.TEXT, ColumnType.TEXT);
    }

    @Test
    void totalsAmountAndSessionsOfTheValidPayments() {
        assertThat(report.total()).hasValueSatisfying(total -> {
            assertThat(total.label()).isEqualTo("Total (valid)");
            assertThat(total.sumHeaders()).containsExactly("Amount", "Sessions");
        });
    }

    @Test
    void labelsEveryMethod() {
        assertThat(PaymentsReport.methodLabel(PaymentMethod.CASH)).isEqualTo("Cash");
        assertThat(PaymentsReport.methodLabel(PaymentMethod.CARD)).isEqualTo("Card");
        assertThat(PaymentsReport.methodLabel(PaymentMethod.BANK_TRANSFER)).isEqualTo("Bank transfer");
        assertThat(PaymentsReport.methodLabel(PaymentMethod.MOBILE_WALLET)).isEqualTo("Mobile wallet");
        assertThat(PaymentsReport.methodLabel(PaymentMethod.OTHER)).isEqualTo("Other");
        assertThat(PaymentsReport.methodLabel(null)).isNull();
    }
}
