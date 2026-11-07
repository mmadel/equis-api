package com.equestrian.equis.common.phone;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

// E17 · per-club sign-in, case 1.
class PhoneNormalizerTest {
    @ParameterizedTest
    @ValueSource(strings = {"+20 100 123 4567", "00201001234567", "201001234567", "010-0123-4567", "01001234567",
            " (010) 0123.4567 "})
    void egyptianMobileNumbersBecomeTheLocalForm(String raw) {
        assertThat(PhoneNormalizer.normalize(raw)).contains("01001234567");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1001234567", "+44 20 7946 0958", "12345", "02123456789", "010012345678", "0100123456a"})
    void anythingElseIsNotAPhone(String raw) {
        assertThat(PhoneNormalizer.normalize(raw)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", " 12345 "})
    void anInvalidPhoneIsKeptTrimmedSoValidationRefusesIt(String raw) {
        assertThat(PhoneNormalizer.cleanForValidation(raw)).isEqualTo("12345");
    }
}
