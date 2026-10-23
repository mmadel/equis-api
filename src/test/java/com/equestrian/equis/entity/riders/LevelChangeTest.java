package com.equestrian.equis.entity.riders;

import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.levels.Level;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LevelChangeTest {
    private final Rider rider = new Rider();
    private final Trainer ahmed = new Trainer();
    private static final Instant AT = Instant.parse("2026-10-15T07:00:00Z");

    @Test
    void ofTrimsTheReason() {
        LevelChange change = LevelChange.of(rider, level(3), level(4), "  Ready for canter ", ahmed, AT);

        assertThat(change.getReason()).isEqualTo("Ready for canter");
        assertThat(change.getRider()).isSameAs(rider);
        assertThat(change.getByTrainer()).isSameAs(ahmed);
        assertThat(change.getCreatedAt()).isEqualTo(AT);
    }

    @Test
    void aStepUpIsAPromotionAndAStepDownIsNot() {
        assertThat(LevelChange.of(rider, level(3), level(4), "Ready for canter", ahmed, AT).isPromotion()).isTrue();
        assertThat(LevelChange.of(rider, level(4), level(3), "Lost confidence", ahmed, AT).isPromotion()).isFalse();
    }

    @Test
    void ofRefusesAnythingButOneStep() {
        assertThatThrownBy(() -> LevelChange.of(rider, level(3), level(5), "Skip", ahmed, AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LevelChange.of(rider, level(3), level(3), "Stay", ahmed, AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ofRefusesABlankReason() {
        assertThatThrownBy(() -> LevelChange.of(rider, level(3), level(4), "   ", ahmed, AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LevelChange.of(rider, level(3), level(4), null, ahmed, AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static Level level(int id) {
        Level level = new Level();
        level.setId((short) id);
        return level;
    }
}
