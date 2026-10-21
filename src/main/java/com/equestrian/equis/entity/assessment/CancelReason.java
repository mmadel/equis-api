package com.equestrian.equis.entity.assessment;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

// Why the admin cancelled an assessment. Only OTHER takes a typed explanation.
public enum CancelReason {
    PERSON_CANCELLED,
    PERSON_UNWELL,
    TRAINER_UNAVAILABLE,
    WEATHER,
    HORSE_UNAVAILABLE,
    FACILITY_CLOSED,
    OTHER;

    // The stored form, also sent to the apps: "person_unwell", ...
    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    // For request parsing: empty when the code is unknown.
    public static Optional<CancelReason> fromCode(String code) {
        return Arrays.stream(values()).filter(reason -> reason.value().equals(code)).findFirst();
    }

    @Converter(autoApply = true)
    public static class DbConverter implements AttributeConverter<CancelReason, String> {
        @Override
        public String convertToDatabaseColumn(CancelReason reason) {
            return reason == null ? null : reason.value();
        }

        @Override
        public CancelReason convertToEntityAttribute(String value) {
            return value == null ? null : CancelReason.valueOf(value.toUpperCase(Locale.ROOT));
        }
    }
}
