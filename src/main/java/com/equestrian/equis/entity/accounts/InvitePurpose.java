package com.equestrian.equis.entity.accounts;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

public enum InvitePurpose {
    INVITE,
    RESET;

    @Converter(autoApply = true)
    public static class DbConverter implements AttributeConverter<InvitePurpose, String> {
        @Override
        public String convertToDatabaseColumn(InvitePurpose purpose) {
            return purpose == null ? null : purpose.name().toLowerCase(Locale.ROOT);
        }

        @Override
        public InvitePurpose convertToEntityAttribute(String value) {
            return value == null ? null : InvitePurpose.valueOf(value.toUpperCase(Locale.ROOT));
        }
    }
}
