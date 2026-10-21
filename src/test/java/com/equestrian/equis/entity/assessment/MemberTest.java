package com.equestrian.equis.entity.assessment;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    // Status task, case 6
    @Test
    void closeThenReopen() {
        Member member = Member.register("Omar", "01011112222", "omar@mail.com");
        Instant closedOn = Instant.parse("2026-10-20T09:00:00Z");
        Instant reopenedOn = Instant.parse("2026-12-01T09:00:00Z");

        member.close(closedOn);
        assertThat(member.isClosed()).isTrue();
        assertThat(member.getClosedAt()).isEqualTo(closedOn);
        assertThat(member.getUpdatedAt()).isEqualTo(closedOn);

        member.reopen(reopenedOn);
        assertThat(member.isClosed()).isFalse();
        assertThat(member.getClosedAt()).isNull();
        assertThat(member.getUpdatedAt()).isEqualTo(reopenedOn);
    }

    // Status task, case 7
    @Test
    void closeTwiceOrReopenAnOpenMemberFails() {
        Member member = Member.register("Omar", "01011112222", "omar@mail.com");
        Instant now = Instant.parse("2026-10-20T09:00:00Z");

        assertThatThrownBy(() -> member.reopen(now)).isInstanceOf(IllegalStateException.class);

        member.close(now);
        assertThatThrownBy(() -> member.close(now)).isInstanceOf(IllegalStateException.class);
        assertThat(member.getClosedAt()).isEqualTo(now);
    }
}
