package com.equestrian.equis.entity.assessment;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class CancelReasonTest {
    // Case 8
    @Test
    void fromCodeKnowsTheSevenCodes() {
        assertThat(CancelReason.fromCode("horse_unavailable")).contains(CancelReason.HORSE_UNAVAILABLE);
        assertThat(CancelReason.fromCode("x")).isEmpty();
        assertThat(CancelReason.fromCode(null)).isEmpty();
        assertThat(CancelReason.fromCode("WEATHER")).isEmpty();
    }

    @Test
    void storedAndSerialisedLowercase() {
        assertThat(new CancelReason.DbConverter().convertToDatabaseColumn(CancelReason.PERSON_UNWELL))
                .isEqualTo("person_unwell");
        assertThat(new CancelReason.DbConverter().convertToEntityAttribute("facility_closed"))
                .isEqualTo(CancelReason.FACILITY_CLOSED);
        assertThat(JsonMapper.builder().build().writeValueAsString(CancelReason.TRAINER_UNAVAILABLE))
                .isEqualTo("\"trainer_unavailable\"");
    }
}
