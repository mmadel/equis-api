package com.equestrian.equis.repository.levels;

import com.equestrian.equis.entity.levels.Level;
import org.flywaydb.core.Flyway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class LevelRepositoryTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    LevelRepository levelRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    Flyway flyway;

    @Test
    void seedsExactlySixLevels() {
        assertThat(levelRepository.findAll())
                .extracting(Level::getId)
                .containsExactlyInAnyOrder((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6);
    }

    @ParameterizedTest
    @CsvSource({
            "1, Level 1, #17695B",
            "2, Level 2, #2A559A",
            "3, Level 3, #5E4196",
            "4, Level 4, #9E4A17",
            "5, Level 5, #96283F",
            "6, Level 6, #4F5B1C"
    })
    void eachLevelHasItsNameAndColour(short id, String name, String colour) {
        Level level = levelRepository.findById(id).orElseThrow();
        assertThat(level.getName()).isEqualTo(name);
        assertThat(level.getColour()).isEqualTo(colour);
    }

    @ParameterizedTest
    @ValueSource(shorts = {0, 7})
    void findByIdOutsideRangeIsEmpty(short id) {
        assertThat(levelRepository.findById(id)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7})
    void rejectsIdOutsideOneToSix(int id) {
        assertThatThrownBy(() -> insert(id, "Level X", "#000000"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_level_id");
    }

    @Test
    void rejectsNonHexColour() {
        jdbcTemplate.update("DELETE FROM level WHERE id = 6");
        assertThatThrownBy(() -> insert(6, "Level 6", "red"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_level_colour");
    }

    @Test
    void rejectsDuplicateName() {
        jdbcTemplate.update("DELETE FROM level WHERE id = 6");
        assertThatThrownBy(() -> insert(6, "Level 1", "#4F5B1C"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_level_name");
    }

    @Test
    void flywayAppliedCreateAndSeed() {
        assertThat(flyway.info().applied())
                .extracting(m -> m.getVersion().getVersion())
                .contains("1", "2");
    }

    private void insert(int id, String name, String colour) {
        jdbcTemplate.update("INSERT INTO level (id, name, colour) VALUES (?, ?, ?)", id, name, colour);
    }
}
