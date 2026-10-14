package com.equestrian.equis.entity.assessment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {
    @Test
    void registerTrimsTheName() {
        Member member = Member.register("  Omar Khaled ", "01012345678");

        assertThat(member.getName()).isEqualTo("Omar Khaled");
        assertThat(member.getPhone()).isEqualTo("01012345678");
    }

    @Test
    void aBlankOrMissingPhoneIsStoredAsNull() {
        assertThat(Member.register("Omar", "   ").getPhone()).isNull();
        assertThat(Member.register("Omar", "").getPhone()).isNull();
        assertThat(Member.register("Omar", null).getPhone()).isNull();
    }
}
