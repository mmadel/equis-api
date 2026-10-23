package com.equestrian.equis.repository.riders;

import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.levels.Level;
import com.equestrian.equis.entity.riders.LevelChange;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.levels.LevelRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Statistics are on so the history test can count SQL statements.
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class LevelChangeRepositoryTest {
    private static final short LEVEL_3 = 3;
    private static final short LEVEL_4 = 4;
    private static final Instant AT = Instant.parse("2026-10-15T07:00:00Z");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    LevelChangeRepository levelChangeRepository;
    @Autowired
    RiderRepository riderRepository;
    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    LevelRepository levelRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    EntityManager entityManager;

    Trainer ahmed;
    Rider sara;
    Rider omar;

    @BeforeEach
    void setUp() {
        ahmed = trainerRepository.saveAndFlush(trainer());
        sara = riderRepository.saveAndFlush(rider("Sara Adel"));
        omar = riderRepository.saveAndFlush(rider("Omar Khaled"));
    }

    @Test
    void savesAndReadsBackAChange() {
        LevelChange saved = levelChangeRepository.saveAndFlush(
                LevelChange.of(sara, level(LEVEL_3), level(LEVEL_4), "  Ready for canter ", ahmed, AT));
        entityManager.clear();

        LevelChange read = levelChangeRepository.findById(saved.getId()).orElseThrow();
        assertThat(read.getReason()).isEqualTo("Ready for canter");
        assertThat(read.getRider().getId()).isEqualTo(sara.getId());
        assertThat(read.getFromLevel().getId()).isEqualTo(LEVEL_3);
        assertThat(read.getToLevel().getId()).isEqualTo(LEVEL_4);
        assertThat(read.getByTrainer().getId()).isEqualTo(ahmed.getId());
        assertThat(read.getCreatedAt()).isNotNull();
        assertThat(read.isPromotion()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"3, 5", "3, 3"})
    void schemaRejectsAnythingButOneStep(short from, short to) {
        assertThatThrownBy(() -> insert(sara.getId(), from, to, "Skip", ahmed.getId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_level_change_one_step");
    }

    @Test
    void schemaRejectsALevelOffTheLadder() {
        assertThatThrownBy(() -> insert(sara.getId(), (short) 6, (short) 7, "Beyond", ahmed.getId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_level_change_to_level");
    }

    @Test
    void schemaRejectsABlankReason() {
        assertThatThrownBy(() -> insert(sara.getId(), LEVEL_3, LEVEL_4, "   ", ahmed.getId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_level_change_reason");
    }

    @Test
    void schemaRejectsAnUnknownRider() {
        assertThatThrownBy(() -> insert(UUID.randomUUID(), LEVEL_3, LEVEL_4, "Ready", ahmed.getId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_level_change_rider");
    }

    @Test
    void schemaRejectsAnUnknownTrainer() {
        assertThatThrownBy(() -> insert(sara.getId(), LEVEL_3, LEVEL_4, "Ready", UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_level_change_by_trainer");
    }

    @Test
    void findsARidersHistoryNewestFirstWithTheTrainerInOneStatement() {
        LevelChange up = levelChangeRepository.saveAndFlush(
                LevelChange.of(sara, level(LEVEL_3), level(LEVEL_4), "Ready for canter", ahmed, AT));
        LevelChange down = levelChangeRepository.saveAndFlush(
                LevelChange.of(sara, level(LEVEL_4), level(LEVEL_3), "Lost confidence", ahmed, AT));
        levelChangeRepository.saveAndFlush(
                LevelChange.of(omar, level(LEVEL_3), level(LEVEL_4), "Ready for canter", ahmed, AT));
        setCreatedAt(up, "2026-10-15T09:00:00Z");
        setCreatedAt(down, "2026-11-02T09:00:00Z");
        entityManager.clear();

        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        List<LevelChange> history = levelChangeRepository.findHistory(sara.getId());

        assertThat(history).containsExactly(down, up);
        assertThat(history).allSatisfy(change -> {
            assertThat(Hibernate.isInitialized(change.getByTrainer())).isTrue();
            assertThat(change.getByTrainer().getName()).isEqualTo("Ahmed Samir");
        });
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void movingARiderUpdatesItsLevelAndVersion() {
        sara.moveToLevel(level(LEVEL_4));
        riderRepository.saveAndFlush(sara);

        assertThat(jdbcTemplate.queryForObject("SELECT level_id FROM rider WHERE id = ?", Short.class, sara.getId()))
                .isEqualTo(LEVEL_4);
        assertThat(jdbcTemplate.queryForObject("SELECT version FROM rider WHERE id = ?", Long.class, sara.getId()))
                .isEqualTo(1L);
    }

    private void insert(UUID riderId, short from, short to, String reason, UUID trainerId) {
        jdbcTemplate.update("""
                INSERT INTO level_change (rider_id, from_level_id, to_level_id, reason, by_trainer_id)
                VALUES (?, ?, ?, ?, ?)
                """, riderId, from, to, reason, trainerId);
    }

    private void setCreatedAt(LevelChange change, String at) {
        jdbcTemplate.update("UPDATE level_change SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse(at)), change.getId());
    }

    private Level level(short id) {
        return levelRepository.getReferenceById(id);
    }

    private Rider rider(String name) {
        Rider rider = new Rider();
        rider.setName(name);
        rider.setLevel(level(LEVEL_3));
        rider.setTrainer(ahmed);
        return rider;
    }

    private Trainer trainer() {
        Trainer trainer = new Trainer();
        trainer.setName("Ahmed Samir");
        trainer.setPhone("01999990011");
        trainer.setEmail("level-change-test-ahmed@example.com");
        return trainer;
    }
}
