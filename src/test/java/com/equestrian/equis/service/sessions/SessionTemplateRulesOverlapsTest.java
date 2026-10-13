package com.equestrian.equis.service.sessions;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

public class SessionTemplateRulesOverlapsTest {

    @Test
    void sameRangeOverlaps() {
        assertThat(overlaps("16:00", "17:00", "16:00", "17:00")).isTrue();
    }

    @Test
    void partlySharedRangeOverlapsFromEitherSide() {
        assertThat(overlaps("16:00", "17:00", "16:30", "17:30")).isTrue();
        assertThat(overlaps("16:30", "17:30", "16:00", "17:00")).isTrue();
    }

    @Test
    void rangeInsideAnotherOverlapsFromEitherSide() {
        assertThat(overlaps("16:00", "18:00", "16:30", "17:00")).isTrue();
        assertThat(overlaps("16:30", "17:00", "16:00", "18:00")).isTrue();
    }

    @Test
    void sharingASingleMinuteOverlaps() {
        assertThat(overlaps("16:00", "17:00", "16:59", "18:00")).isTrue();
    }

    @Test
    void rangesThatOnlyTouchDoNotOverlap() {
        assertThat(overlaps("16:00", "17:00", "17:00", "18:00")).isFalse();
        assertThat(overlaps("17:00", "18:00", "16:00", "17:00")).isFalse();
    }

    @Test
    void separateRangesDoNotOverlap() {
        assertThat(overlaps("09:00", "10:00", "16:00", "17:00")).isFalse();
        assertThat(overlaps("16:00", "17:00", "09:00", "10:00")).isFalse();
    }

    private static boolean overlaps(String aStart, String aEnd, String bStart, String bEnd) {
        return SessionTemplateRules.overlaps(LocalTime.parse(aStart), LocalTime.parse(aEnd),
                LocalTime.parse(bStart), LocalTime.parse(bEnd));
    }
}
