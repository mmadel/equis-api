package com.equestrian.equis.entity.payments;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentEnumsTest {
    private final JsonMapper json = JsonMapper.builder().build();

    // Case 17
    @Test
    void serialisedLowercase() {
        assertThat(json.writeValueAsString(PaymentMethod.BANK_TRANSFER)).isEqualTo("\"bank_transfer\"");
        assertThat(json.writeValueAsString(RiderPaymentStatus.AWAITING_PAYMENT)).isEqualTo("\"awaiting_payment\"");
        assertThat(json.writeValueAsString(RiderPaymentStatus.ACTIVE)).isEqualTo("\"active\"");
    }

    @Test
    void methodStoredLowercase() {
        PaymentMethod.DbConverter converter = new PaymentMethod.DbConverter();
        assertThat(converter.convertToDatabaseColumn(PaymentMethod.MOBILE_WALLET)).isEqualTo("mobile_wallet");
        assertThat(converter.convertToEntityAttribute("bank_transfer")).isEqualTo(PaymentMethod.BANK_TRANSFER);
    }

    @Test
    void fromCodeKnowsTheFiveMethods() {
        assertThat(PaymentMethod.fromCode("cash")).contains(PaymentMethod.CASH);
        assertThat(PaymentMethod.fromCode("other")).contains(PaymentMethod.OTHER);
        assertThat(PaymentMethod.fromCode("paypal")).isEmpty();
        assertThat(PaymentMethod.fromCode("CASH")).isEmpty();
        assertThat(PaymentMethod.fromCode(null)).isEmpty();
    }

    @Test
    void statusFollowsTheBalance() {
        assertThat(RiderPaymentStatus.fromBalance(1)).isEqualTo(RiderPaymentStatus.ACTIVE);
        assertThat(RiderPaymentStatus.fromBalance(0)).isEqualTo(RiderPaymentStatus.AWAITING_PAYMENT);
    }
}
