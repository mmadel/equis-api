package com.equestrian.equis.entity.payments;

import com.equestrian.equis.entity.riders.Rider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {
    private static final UUID ADMIN = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-03T08:00:00Z");
    private static final BigDecimal AMOUNT = new BigDecimal("1200.00");
    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private static final YearMonth DEC = YearMonth.of(2026, 12);

    private final Rider sara = new Rider();

    // Case 2
    @Test
    void oneMonthIsValidThroughTheGraceMonth() {
        Payment payment = record(NOV, 1, 8);

        assertThat(payment.getStartMonth()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(payment.getValidUntil()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(payment.getRecordedAt()).isEqualTo(NOW);
        assertThat(payment.getRecordedBy()).isEqualTo(ADMIN);
        assertThat(payment.isVoided()).isFalse();
    }

    // Case 3
    @Test
    void validUntilIsTheLastDayOfTheMonthAfterTheLastPaidMonth() {
        assertThat(record(NOV, 2, 16).getValidUntil()).isEqualTo(LocalDate.of(2027, 1, 31));
        assertThat(record(DEC, 1, 8).getValidUntil()).isEqualTo(LocalDate.of(2027, 1, 31));
        // The grace month ends on 28 February in a non-leap year.
        assertThat(record(YearMonth.of(2026, 12), 2, 16).getValidUntil()).isEqualTo(LocalDate.of(2027, 2, 28));
    }

    // Case 4
    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-5"})
    void rejectsAnAmountThatIsNotAbove0(String amount) {
        assertThatThrownBy(() -> Payment.record(sara, new BigDecimal(amount), PaymentMethod.CASH, NOV, 1, 8, ADMIN, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // Case 4
    @ParameterizedTest
    @CsvSource({"0, 8", "13, 8", "1, 0", "1, 201"})
    void rejectsMonthsOrSessionsOutOfRange(int months, int sessions) {
        assertThatThrownBy(() -> record(NOV, months, sessions)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsTheEdgesOfTheRanges() {
        assertThat(record(NOV, 12, 200).getMonths()).isEqualTo(12);
        assertThat(record(NOV, 1, 1).getSessions()).isEqualTo(1);
    }

    @Test
    void defaultSessionsAre8PerMonth() {
        assertThat(Payment.defaultSessions(1)).isEqualTo(8);
        assertThat(Payment.defaultSessions(3)).isEqualTo(24);
    }

    // Decision 7: a package counts only from its start month, through the grace month.
    @Test
    void isValidFromTheStartMonthToValidUntil() {
        Payment payment = record(DEC, 1, 8);

        assertThat(payment.isValidOn(LocalDate.of(2026, 11, 30))).isFalse();
        assertThat(payment.isValidOn(LocalDate.of(2026, 12, 1))).isTrue();
        assertThat(payment.isValidOn(LocalDate.of(2027, 1, 31))).isTrue();
        assertThat(payment.isValidOn(LocalDate.of(2027, 2, 1))).isFalse();
    }

    @Test
    void voidingTrimsTheReasonAndStopsTheSessionsCounting() {
        Payment payment = record(NOV, 1, 8);
        Instant later = NOW.plusSeconds(60);

        payment.voidPayment(ADMIN, "  Entered twice ", later);

        assertThat(payment.isVoided()).isTrue();
        assertThat(payment.getVoidReason()).isEqualTo("Entered twice");
        assertThat(payment.getVoidedBy()).isEqualTo(ADMIN);
        assertThat(payment.getVoidedAt()).isEqualTo(later);
        assertThat(payment.isValidOn(LocalDate.of(2026, 11, 10))).isFalse();
    }

    // Case 13
    @Test
    void voidingTwiceIsRefused() {
        Payment payment = record(NOV, 1, 8);
        payment.voidPayment(ADMIN, "Entered twice", NOW);

        assertThatThrownBy(() -> payment.voidPayment(ADMIN, "Again", NOW)).isInstanceOf(IllegalStateException.class);
        assertThat(payment.getVoidReason()).isEqualTo("Entered twice");
    }

    // Case 13
    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void voidingWithABlankReasonIsRefusedAndChangesNothing(String reason) {
        Payment payment = record(NOV, 1, 8);

        assertThatThrownBy(() -> payment.voidPayment(ADMIN, reason, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> payment.voidPayment(ADMIN, null, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThat(payment.isVoided()).isFalse();
    }

    private Payment record(YearMonth start, int months, int sessions) {
        return Payment.record(sara, AMOUNT, PaymentMethod.CASH, start, months, sessions, ADMIN, NOW);
    }
}
