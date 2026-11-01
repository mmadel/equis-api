package com.equestrian.equis.entity.riders;

import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.assessment.Member;
import com.equestrian.equis.entity.levels.Level;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static com.equestrian.equis.entity.riders.LevelChangeTest.level;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiderTest {
    @Test
    void movesOneLevelUpOrDown() {
        Rider rider = inLevel(3);
        Level four = level(4);

        rider.moveToLevel(four);
        assertThat(rider.getLevel()).isSameAs(four);

        rider.moveToLevel(level(3));
        assertThat(rider.getLevel().getId()).isEqualTo((short) 3);
    }

    @Test
    void refusesToSkipOrStayAndKeepsTheLevel() {
        Rider rider = inLevel(3);
        Level three = rider.getLevel();

        assertThatThrownBy(() -> rider.moveToLevel(level(5))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rider.moveToLevel(level(3))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rider.moveToLevel(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(rider.getLevel()).isSameAs(three);
    }

    @Test
    void assignsATrainerAndSaysWhenNothingChanged() {
        Rider rider = inLevel(2);
        Trainer ahmed = trainer();
        Trainer mona = trainer();

        assertThat(rider.assignTrainer(ahmed)).isTrue();
        assertThat(rider.assignTrainer(mona)).isTrue();
        assertThat(rider.getTrainer()).isSameAs(mona);
        assertThat(rider.assignTrainer(mona)).isFalse();
        assertThatThrownBy(() -> rider.assignTrainer(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(rider.getTrainer()).isSameAs(mona);
    }

    // E15 case 10
    @Test
    void fromAssessmentCopiesTheMembersEmail() {
        Member omar = Member.register("Omar Khaled", "01012345678", " Omar.Khaled@Gmail.com ");

        Rider rider = Rider.fromAssessment(omar, level(2), trainer());

        assertThat(rider.getEmail()).isEqualTo("omar.khaled@gmail.com");
        assertThat(rider.getAccountStatus()).isEqualTo(RiderAccountStatus.NONE);
        assertThat(rider.hasAccount()).isFalse();
    }

    // E15 case 11
    @Test
    void markInvitedFromNoneOrInvited() {
        Rider rider = withEmail();

        rider.markInvited();
        assertThat(rider.getAccountStatus()).isEqualTo(RiderAccountStatus.INVITED);
        rider.markInvited();
        assertThat(rider.getAccountStatus()).isEqualTo(RiderAccountStatus.INVITED);
    }

    @Test
    void markInvitedRefusesARiderWithoutEmailOrAlreadyActive() {
        Rider noEmail = inLevel(1);
        assertThatThrownBy(noEmail::markInvited).isInstanceOf(IllegalStateException.class);
        assertThat(noEmail.getAccountStatus()).isEqualTo(RiderAccountStatus.NONE);

        Rider active = withEmail();
        active.markInvited();
        active.setPassword("hash-1");
        assertThatThrownBy(active::markInvited).isInstanceOf(IllegalStateException.class);
        assertThat(active.getAccountStatus()).isEqualTo(RiderAccountStatus.ACTIVE);
    }

    // E15 case 12
    @Test
    void setPasswordActivatesAnInvitedRiderAndKeepsAnActiveOneActive() {
        Rider rider = withEmail();
        rider.markInvited();

        rider.setPassword("hash-1");
        assertThat(rider.getAccountStatus()).isEqualTo(RiderAccountStatus.ACTIVE);
        assertThat(rider.getPasswordHash()).isEqualTo("hash-1");
        assertThat(rider.hasAccount()).isTrue();

        rider.setPassword("hash-2");
        assertThat(rider.getAccountStatus()).isEqualTo(RiderAccountStatus.ACTIVE);
        assertThat(rider.getPasswordHash()).isEqualTo("hash-2");
    }

    @Test
    void setPasswordRefusesARiderNeverInvited() {
        Rider rider = withEmail();

        assertThatThrownBy(() -> rider.setPassword("hash-1")).isInstanceOf(IllegalStateException.class);
        assertThat(rider.getPasswordHash()).isNull();
        assertThat(rider.getAccountStatus()).isEqualTo(RiderAccountStatus.NONE);
    }

    @Test
    void touchSetsLastActive() {
        Rider rider = withEmail();
        Instant now = Instant.parse("2026-11-03T15:00:00Z");

        rider.touch(now);

        assertThat(rider.getLastActiveAt()).isEqualTo(now);
    }

    private Rider withEmail() {
        Rider rider = inLevel(1);
        rider.setEmail("sara.ali@gmail.com");
        return rider;
    }

    private static Trainer trainer() {
        Trainer trainer = new Trainer();
        trainer.setId(UUID.randomUUID());
        return trainer;
    }

    private Rider inLevel(int id) {
        Rider rider = new Rider();
        rider.setLevel(level(id));
        return rider;
    }
}
