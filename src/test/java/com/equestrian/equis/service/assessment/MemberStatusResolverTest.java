package com.equestrian.equis.service.assessment;

import com.equestrian.equis.entity.assessment.MemberStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

// E11 · Track where each person is (status): cases 2 to 5.
class MemberStatusResolverTest {
    // Case 2: closed wins over the last booking.
    @Test
    void closedWinsOverEverything() {
        assertThat(MemberStatusResolver.resolve(true, false, "noshow")).isEqualTo(MemberStatus.CLOSED);
    }

    // Case 3
    @Test
    void aRiderOrAPlacedBookingIsRider() {
        assertThat(MemberStatusResolver.resolve(false, true, "placed")).isEqualTo(MemberStatus.RIDER);
        assertThat(MemberStatusResolver.resolve(false, false, "placed")).isEqualTo(MemberStatus.RIDER);
    }

    // Case 4
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"upcoming", "waiting", "cancelled"})
    void otherwiseWaitingForAssessment(String latestDisplayStatus) {
        assertThat(MemberStatusResolver.resolve(false, false, latestDisplayStatus))
                .isEqualTo(MemberStatus.PENDING_ASSESSMENT);
    }

    // Case 5
    @Test
    void aNoShowIsNoShow() {
        assertThat(MemberStatusResolver.resolve(false, false, "noshow")).isEqualTo(MemberStatus.NO_SHOW);
    }
}
