package com.equestrian.equis.entity.riders;

import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.levels.Level;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LevelChangeTest {
    private final Rider rider = new Rider();
    private final Trainer ahmed = new Trainer();

    @Test
    void ofTrimsTheReason() {
        LevelChange change = LevelChange.of(rider, level(3), level(4), "  Ready for canter ", ahmed);

        assertThat(change.getReason()).isEqualTo("Ready for canter");
        assertThat(change.getRider()).isSameAs(rider);
        assertThat(change.getByTrainer()).isSameAs(ahmed);
    }

    @Test
    void aStepUpIsAPromotionAndAStepDownIsNot() {
        assertThat(LevelChange.of(rider, level(3), level(4), "Ready for canter", ahmed).isPromotion()).isTrue();
        assertThat(LevelChange.of(rider, level(4), level(3), "Lost confidence", ahmed).isPromotion()).isFalse();
    }

    @Test
    void ofRefusesAnythingButOneStep() {
        assertThatThrownBy(() -> LevelChange.of(rider, level(3), level(5), "Skip", ahmed))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LevelChange.of(rider, level(3), level(3), "Stay", ahmed))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ofRefusesABlankReason() {
        assertThatThrownBy(() -> LevelChange.of(rider, level(3), level(4), "   ", ahmed))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LevelChange.of(rider, level(3), level(4), null, ahmed))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static Level level(int id) {
        Level level = new Level();
        level.setId((short) id);
        return level;
    }
}
