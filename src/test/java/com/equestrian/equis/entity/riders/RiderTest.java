package com.equestrian.equis.entity.riders;

import com.equestrian.equis.entity.levels.Level;
import org.junit.jupiter.api.Test;

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

    private Rider inLevel(int id) {
        Rider rider = new Rider();
        rider.setLevel(level(id));
        return rider;
    }
}
