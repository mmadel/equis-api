package com.equestrian.equis.service.platform;

import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.entity.club.ClubStatus;
import com.equestrian.equis.entity.platform.BillingPeriod;
import com.equestrian.equis.entity.platform.OnExpiry;
import com.equestrian.equis.entity.platform.Plan;
import com.equestrian.equis.entity.platform.Subscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// E17 · club access state (case 8) and days left (case 9). Pure logic: no Spring, no database.
class ClubAccessServiceTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // Sat 7 Nov 2026, 15:00 Cairo.
    private static final Instant NOW = ZonedDateTime.of(2026, 11, 7, 15, 0, 0, 0, CAIRO).toInstant();
    private static final Instant DEC_7 = ZonedDateTime.of(2026, 12, 7, 15, 0, 0, 0, CAIRO).toInstant();

    private final ClubAccessService service =
            new ClubAccessService(null, null, CAIRO, Clock.fixed(NOW, CAIRO));

    private final Plan trial = plan("TRIAL", 30, OnExpiry.BLOCK);
    private final Plan basic = plan("BASIC", null, OnExpiry.READ_ONLY);

    // Case 8
    @Test
    void aSuspendedClubIsBlockedEvenWithAValidSubscription() {
        Club club = club(ClubStatus.SUSPENDED);

        ClubAccess access = service.stateOf(club, Optional.of(trialOf(club, NOW, DEC_7)), NOW);

        assertThat(access.state()).isEqualTo(AccessState.BLOCKED);
        assertThat(access.reason()).isEqualTo(AccessReason.SUSPENDED);
        assertThat(access.plan()).isSameAs(trial);
        assertThat(access.daysLeft()).isEqualTo(30);
    }

    @Test
    void aSuspendedClubWithoutASubscriptionIsBlockedAsSuspended() {
        ClubAccess access = service.stateOf(club(ClubStatus.SUSPENDED), Optional.empty(), NOW);

        assertThat(access.state()).isEqualTo(AccessState.BLOCKED);
        assertThat(access.reason()).isEqualTo(AccessReason.SUSPENDED);
    }

    @Test
    void noSubscriptionIsBlocked() {
        ClubAccess access = service.stateOf(club(ClubStatus.ACTIVE), Optional.empty(), NOW);

        assertThat(access).isEqualTo(
                new ClubAccess(AccessState.BLOCKED, AccessReason.NO_SUBSCRIPTION, null, null, null, 0));
    }

    @Test
    void anEndedTrialBlocksTheClub() {
        Club club = club(ClubStatus.ACTIVE);
        Instant start = NOW.minusSeconds(40L * 24 * 3600);

        ClubAccess access = service.stateOf(club, Optional.of(trialOf(club, start, NOW.minusSeconds(1))), NOW);

        assertThat(access.state()).isEqualTo(AccessState.BLOCKED);
        assertThat(access.reason()).isEqualTo(AccessReason.TRIAL_ENDED);
        assertThat(access.daysLeft()).isZero();
    }

    @Test
    void anEndedBasicSubscriptionMakesTheClubReadOnly() {
        Club club = club(ClubStatus.ACTIVE);
        Subscription ended = Subscription.create(club.getId(), basic, BillingPeriod.MONTHLY,
                NOW.minusSeconds(31L * 24 * 3600), NOW, null, UUID.randomUUID());

        ClubAccess access = service.stateOf(club, Optional.of(ended), NOW);

        assertThat(access.state()).isEqualTo(AccessState.READ_ONLY);
        assertThat(access.reason()).isEqualTo(AccessReason.SUBSCRIPTION_ENDED);
        assertThat(access.period()).isEqualTo(BillingPeriod.MONTHLY);
    }

    @Test
    void aValidSubscriptionIsActive() {
        Club club = club(ClubStatus.ACTIVE);

        ClubAccess access = service.stateOf(club, Optional.of(trialOf(club, NOW, DEC_7)), NOW);

        assertThat(access).isEqualTo(
                new ClubAccess(AccessState.ACTIVE, null, trial, BillingPeriod.TRIAL, DEC_7, 30));
    }

    // Case 9: the trial runs 7 Nov 15:00 -> 7 Dec 15:00 Cairo.
    @ParameterizedTest
    @CsvSource({
            "2026-11-07T15:00, 30",
            "2026-12-06T15:00, 1",
            "2026-12-06T16:00, 0",
            "2026-12-07T15:00, 0",
            "2026-12-07T16:00, 0"})
    void daysLeftAreWholeDaysRoundedDown(LocalDateTime cairoTime, int expected) {
        assertThat(service.daysLeft(cairoTime.atZone(CAIRO).toInstant(), DEC_7)).isEqualTo(expected);
    }

    private Subscription trialOf(Club club, Instant start, Instant end) {
        return Subscription.create(club.getId(), trial, BillingPeriod.TRIAL, start, end, null, UUID.randomUUID());
    }

    private static Club club(ClubStatus status) {
        Club club = new Club();
        club.setId(UUID.randomUUID());
        club.setCode("cairo-eq");
        club.setName("Cairo Equestrian");
        club.setStatus(status);
        return club;
    }

    // Plan rows only come from the ops script, so the entity has no public constructor or setters.
    private static Plan plan(String code, Integer trialDays, OnExpiry onExpiry) {
        Plan plan = BeanUtils.instantiateClass(Plan.class);
        ReflectionTestUtils.setField(plan, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(plan, "code", code);
        ReflectionTestUtils.setField(plan, "name", code.charAt(0) + code.substring(1).toLowerCase());
        ReflectionTestUtils.setField(plan, "trialDays", trialDays);
        ReflectionTestUtils.setField(plan, "onExpiry", onExpiry);
        return plan;
    }
}
