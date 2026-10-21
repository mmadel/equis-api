package com.equestrian.equis.entity.assessment;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class MemberStatusTest {
    // Case 8
    @Test
    void serialisesLowercase() {
        JsonMapper json = JsonMapper.builder().build();

        assertThat(json.writeValueAsString(MemberStatus.NO_SHOW)).isEqualTo("\"no_show\"");
        assertThat(json.writeValueAsString(MemberStatus.PENDING_ASSESSMENT)).isEqualTo("\"pending_assessment\"");
    }
}
