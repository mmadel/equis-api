package com.equestrian.equis.entity.assessment;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

public enum BookingStatus {
    UPCOMING,
    PLACED,
    NOSHOW,
    CANCELLED;

    // The stored form, also sent to the apps: "upcoming", "placed", ...
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Converter(autoApply = true)
    public static class DbConverter implements AttributeConverter<BookingStatus, String> {
        @Override
        public String convertToDatabaseColumn(BookingStatus status) {
            return status == null ? null : status.value();
        }

        @Override
        public BookingStatus convertToEntityAttribute(String value) {
            return value == null ? null : BookingStatus.valueOf(value.toUpperCase(Locale.ROOT));
        }
    }
}
