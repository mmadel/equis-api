package com.equestrian.equis.entity.payments;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

// How a rider paid. Every amount is in EGP.
public enum PaymentMethod {
    CASH,
    CARD,
    BANK_TRANSFER,
    MOBILE_WALLET,
    OTHER;

    // The stored form, also sent to the apps: "cash", "bank_transfer", ...
    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    // For request parsing: empty when the code is unknown.
    public static Optional<PaymentMethod> fromCode(String code) {
        return Arrays.stream(values()).filter(method -> method.value().equals(code)).findFirst();
    }

    @Converter(autoApply = true)
    public static class DbConverter implements AttributeConverter<PaymentMethod, String> {
        @Override
        public String convertToDatabaseColumn(PaymentMethod method) {
            return method == null ? null : method.value();
        }

        @Override
        public PaymentMethod convertToEntityAttribute(String value) {
            return value == null ? null : PaymentMethod.valueOf(value.toUpperCase(Locale.ROOT));
        }
    }
}
