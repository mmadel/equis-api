package com.equestrian.equis.entity.assessment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {
    // Case 3 (cleaning part)
    @Test
    void registerTrimsAllThreeAndLowercasesTheEmail() {
        Member member = Member.register("  Sara ", " 01011112222 ", " Sara@Mail.COM ");

        assertThat(member.getName()).isEqualTo("Sara");
        assertThat(member.getPhone()).isEqualTo("01011112222");
        assertThat(member.getEmail()).isEqualTo("sara@mail.com");
    }

    // Case 7 (cleaning part)
    @Test
    void updateDetailsCleansLikeRegister() {
        Member member = Member.register("Sara", "01011112222", "sara@mail.com");

        member.updateDetails(" Sara Ali ", " 01233334444 ", " SARA.ALI@Mail.com ");

        assertThat(member.getName()).isEqualTo("Sara Ali");
        assertThat(member.getPhone()).isEqualTo("01233334444");
        assertThat(member.getEmail()).isEqualTo("sara.ali@mail.com");
    }
}
