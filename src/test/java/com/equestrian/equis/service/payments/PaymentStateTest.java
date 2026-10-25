package com.equestrian.equis.service.payments;

import com.equestrian.equis.entity.payments.Payment;
import com.equestrian.equis.entity.payments.PaymentMethod;
import com.equestrian.equis.entity.riders.Rider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Case 11 of "Admin records a payment": the order the states are checked in, and what is left in each.
class PaymentStateTest {
    private static final UUID ADMIN = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-03T08:15:00Z");
    // November, 1 month, 8 sessions: valid 1 Nov to 31 Dec.
    private final Payment nov = Payment.record(new Rider(), new BigDecimal("1200.00"), PaymentMethod.CASH,
            YearMonth.of(2026, 11), 1, 8, ADMIN, NOW);

    @Test
    void voidedComesFirst() {
        nov.voidPayment(ADMIN, "Typed the wrong rider", NOW);

        assertThat(PaymentState.of(nov, 0, LocalDate.of(2026, 11, 3))).isEqualTo(PaymentState.VOIDED);
        assertThat(PaymentState.VOIDED.left(nov, 0)).isZero();
    }

    @Test
    void upcomingBeforeTheStartMonth() {
        PaymentState state = PaymentState.of(nov, 0, LocalDate.of(2026, 10, 31));

        assertThat(state).isEqualTo(PaymentState.UPCOMING);
        assertThat(state.left(nov, 0)).isEqualTo(8);
    }

    @Test
    void expiredAfterValidUntilEvenWithSessionsLeft() {
        PaymentState state = PaymentState.of(nov, 3, LocalDate.of(2027, 1, 1));

        assertThat(state).isEqualTo(PaymentState.EXPIRED);
        assertThat(state.left(nov, 3)).isZero();
    }

    @Test
    void usedUpWhenEverySessionIsUsed() {
        PaymentState state = PaymentState.of(nov, 8, LocalDate.of(2026, 12, 31));

        assertThat(state).isEqualTo(PaymentState.USED_UP);
        assertThat(state.left(nov, 8)).isZero();
    }

    @Test
    void activeOtherwise() {
        PaymentState state = PaymentState.of(nov, 3, LocalDate.of(2026, 11, 1));

        assertThat(state).isEqualTo(PaymentState.ACTIVE);
        assertThat(state.left(nov, 3)).isEqualTo(5);
        assertThat(state.value()).isEqualTo("active");
        assertThat(PaymentState.USED_UP.value()).isEqualTo("used_up");
    }
}
