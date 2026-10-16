package com.equestrian.equis.entity.notices;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

public enum NoticeType {
    RESULT_OVERDUE,
    SESSION_CANCELLED;

    // The stored form, also sent to the app: "result_overdue", "session_cancelled".
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Converter(autoApply = true)
    public static class DbConverter implements AttributeConverter<NoticeType, String> {
        @Override
        public String convertToDatabaseColumn(NoticeType type) {
            return type == null ? null : type.value();
        }

        @Override
        public NoticeType convertToEntityAttribute(String value) {
            return value == null ? null : NoticeType.valueOf(value.toUpperCase(Locale.ROOT));
        }
    }
}
