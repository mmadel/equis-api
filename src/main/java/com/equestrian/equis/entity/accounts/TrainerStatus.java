package com.equestrian.equis.entity.accounts;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

public enum TrainerStatus {
    INVITED,
    ACTIVE,
    DISABLED;

    @Converter(autoApply = true)
    public static class DbConverter implements AttributeConverter<TrainerStatus, String> {
        @Override
        public String convertToDatabaseColumn(TrainerStatus status) {
            return status == null ? null : status.name().toLowerCase(Locale.ROOT);
        }

        @Override
        public TrainerStatus convertToEntityAttribute(String value) {
            return value == null ? null : TrainerStatus.valueOf(value.toUpperCase(Locale.ROOT));
        }
    }
}
