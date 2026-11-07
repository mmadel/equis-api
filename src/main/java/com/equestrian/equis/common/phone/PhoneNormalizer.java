package com.equestrian.equis.common.phone;

import java.util.Optional;
import java.util.regex.Pattern;

// Egyptian mobile numbers in the stored local form, 01XXXXXXXXX (spec 4.7).
// "+20 100 123 4567", "00201001234567", "201001234567" and "010-0123-4567" are all "01001234567".
public final class PhoneNormalizer {
    public static final String LOCAL_FORMAT = "^01[0-9]{9}$";

    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\-.()]");
    private static final Pattern LOCAL = Pattern.compile(LOCAL_FORMAT);

    private PhoneNormalizer() {}

    // Empty when the input is not an Egyptian mobile number.
    public static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String phone = SEPARATORS.matcher(raw).replaceAll("");
        if (phone.startsWith("+20")) {
            phone = "0" + phone.substring(3);
        } else if (phone.startsWith("0020")) {
            phone = "0" + phone.substring(4);
        } else if (phone.startsWith("20") && phone.length() == 12) {
            phone = "0" + phone.substring(2);
        }
        return LOCAL.matcher(phone).matches() ? Optional.of(phone) : Optional.empty();
    }

    // For request records: the normalized phone, or the trimmed input when it isn't valid, so the
    // @Pattern(LOCAL_FORMAT) on the field still gives the usual validation error.
    public static String cleanForValidation(String raw) {
        return raw == null ? null : normalize(raw).orElse(raw.trim());
    }
}
