package com.equestrian.equis.entity.accounts;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

public enum SubjectType {
    TRAINER,
    ADMIN,
    RIDER;

    @Converter(autoApply = true)
    public static class DbConverter implements AttributeConverter<SubjectType, String> {
        @Override
        public String convertToDatabaseColumn(SubjectType subjectType) {
            return subjectType == null ? null : subjectType.name().toLowerCase(Locale.ROOT);
        }

        @Override
        public SubjectType convertToEntityAttribute(String value) {
            return value == null ? null : SubjectType.valueOf(value.toUpperCase(Locale.ROOT));
        }
    }
}
