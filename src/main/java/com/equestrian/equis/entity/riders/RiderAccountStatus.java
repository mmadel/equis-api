package com.equestrian.equis.entity.riders;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

// NONE: nothing sent yet. INVITED: a set-password link was sent, no password yet. ACTIVE: can sign in.
public enum RiderAccountStatus {
    NONE,
    INVITED,
    ACTIVE;

    @JsonValue
    public String json() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Converter(autoApply = true)
    public static class DbConverter implements AttributeConverter<RiderAccountStatus, String> {
        @Override
        public String convertToDatabaseColumn(RiderAccountStatus status) {
            return status == null ? null : status.name().toLowerCase(Locale.ROOT);
        }

        @Override
        public RiderAccountStatus convertToEntityAttribute(String value) {
            return value == null ? null : RiderAccountStatus.valueOf(value.toUpperCase(Locale.ROOT));
        }
    }
}
