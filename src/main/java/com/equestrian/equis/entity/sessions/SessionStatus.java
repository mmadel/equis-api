package com.equestrian.equis.entity.sessions;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

public enum SessionStatus {
    SCHEDULED,
    CANCELLED,
    FINISHED,
    // Finished, and every seated rider has an evaluation. Final.
    DONE;

    @Converter(autoApply = true)
    public static class DbConverter implements AttributeConverter<SessionStatus, String> {
        @Override
        public String convertToDatabaseColumn(SessionStatus status) {
            return status == null ? null : status.name().toLowerCase(Locale.ROOT);
        }

        @Override
        public SessionStatus convertToEntityAttribute(String value) {
            return value == null ? null : SessionStatus.valueOf(value.toUpperCase(Locale.ROOT));
        }
    }
}
