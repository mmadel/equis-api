package com.equestrian.equis.repository.riders;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.riders.RiderAccountStatus;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.levels.LevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RiderRepositoryTest {
    private static final short LEVEL_1 = 1;
    private static final short LEVEL_2 = 2;

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @Autowired
    RiderRepository riderRepository;
    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    LevelRepository levelRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    Trainer sara;
    Trainer omar;

    // Start from an empty rider table whatever the migrations seed (rolled back afterwards).
    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM rider");
        sara = trainerRepository.saveAndFlush(trainer("Sara Ali", "01999990001", "rider-test-sara@example.com"));
        omar = trainerRepository.saveAndFlush(trainer("Omar Hassan", "01999990002", "rider-test-omar@example.com"));
    }

    @Test
    void savesWithGeneratedIdVersionAndTimestamps() {
        Rider saved = riderRepository.saveAndFlush(rider("Laila Nour", LEVEL_1, sara));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getPhone()).isNull();
        assertThat(saved.getMemberId()).isNull();
    }

    @Test
    void savesUnassignedRider() {
        Rider saved = riderRepository.saveAndFlush(rider("Laila Nour", LEVEL_1, null));

        assertThat(riderRepository.findById(saved.getId()).orElseThrow().getTrainer()).isNull();
    }

    @Test
    void incrementsVersionOnUpdate() {
        Rider saved = riderRepository.saveAndFlush(rider("Laila Nour", LEVEL_1, sara));

        saved.setName("Laila Nour El Din");
        riderRepository.saveAndFlush(saved);

        assertThat(saved.getVersion()).isEqualTo(1);
    }

    @Test
    void rejectsStaleUpdate() {
        Rider saved = riderRepository.saveAndFlush(rider("Laila Nour", LEVEL_1, sara));
        jdbcTemplate.update("UPDATE rider SET version = version + 1 WHERE id = ?", saved.getId());

        saved.setName("Laila Nour El Din");

        assertThatThrownBy(() -> riderRepository.saveAndFlush(saved))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void rejectsMalformedPhone() {
        Rider rider = rider("Laila Nour", LEVEL_1, sara);
        rider.setPhone("1234");

        assertThatThrownBy(() -> riderRepository.saveAndFlush(rider))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_rider_phone");
    }

    @Test
    void findsATrainersRidersOfALevelOrderedByName() {
        Rider zeina = riderRepository.saveAndFlush(rider("Zeina Fahmy", LEVEL_1, sara));
        Rider adam = riderRepository.saveAndFlush(rider("Adam Samir", LEVEL_1, sara));
        Rider laila = riderRepository.saveAndFlush(rider("Laila Nour", LEVEL_1, sara));
        riderRepository.saveAndFlush(rider("Other Level", LEVEL_2, sara));
        riderRepository.saveAndFlush(rider("Other Trainer", LEVEL_1, omar));
        riderRepository.saveAndFlush(rider("No Trainer", LEVEL_1, null));

        assertThat(riderRepository.findByTrainerIdAndLevelIdOrderByName(sara.getId(), LEVEL_1))
                .containsExactly(adam, laila, zeina);
    }

    @Test
    void countsATrainersRidersPerLevel() {
        riderRepository.saveAndFlush(rider("Adam Samir", LEVEL_1, sara));
        riderRepository.saveAndFlush(rider("Laila Nour", LEVEL_1, sara));
        riderRepository.saveAndFlush(rider("Zeina Fahmy", LEVEL_2, sara));
        riderRepository.saveAndFlush(rider("Other Trainer", LEVEL_2, omar));
        riderRepository.saveAndFlush(rider("No Trainer", LEVEL_2, null));

        assertThat(riderRepository.countByTrainerPerLevel(sara.getId()))
                .extracting(LevelRiderCount::getLevelId, LevelRiderCount::getRiderCount)
                .containsExactly(tuple(LEVEL_1, 2L), tuple(LEVEL_2, 1L));
    }

    @Test
    void countsNothingForATrainerWithoutRiders() {
        riderRepository.saveAndFlush(rider("Adam Samir", LEVEL_1, sara));

        assertThat(riderRepository.countByTrainerPerLevel(omar.getId())).isEmpty();
        assertThat(riderRepository.countByTrainerPerLevel(UUID.randomUUID())).isEmpty();
    }

    // E15 case 3
    @Test
    void rejectsTwoRidersWithTheSameEmail() {
        Rider first = rider("Sara Ali", LEVEL_1, sara);
        first.setEmail("sara.ali@gmail.com");
        riderRepository.saveAndFlush(first);
        Rider second = rider("Sara Ali Hassan", LEVEL_1, omar);
        second.setEmail("sara.ali@gmail.com");

        assertThatThrownBy(() -> riderRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_rider_email");
    }

    // E15 case 4
    @Test
    void rejectsAnEmailThatIsNotLowercase() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO rider (name, level_id, email) VALUES ('Sara Ali', 1, 'Sara@Gmail.com')"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_rider_email");
    }

    @Test
    void newRiderHasNoAccount() {
        Rider saved = riderRepository.saveAndFlush(rider("Laila Nour", LEVEL_1, sara));

        assertThat(jdbcTemplate.queryForObject("SELECT account_status FROM rider WHERE id = ?", String.class, saved.getId()))
                .isEqualTo("none");
        assertThat(saved.getEmail()).isNull();
        assertThat(saved.getPasswordHash()).isNull();
        assertThat(saved.getLastActiveAt()).isNull();
    }

    // E15 case 5
    @Test
    void rejectsAnInvitedRiderWithoutEmail() {
        UUID id = riderRepository.saveAndFlush(rider("Laila Nour", LEVEL_1, sara)).getId();

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE rider SET account_status = 'invited' WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_rider_account_email");
    }

    @Test
    void rejectsAnActiveRiderWithoutPassword() {
        UUID id = riderRepository.saveAndFlush(withEmail("Laila Nour", "laila@gmail.com")).getId();

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE rider SET account_status = 'active' WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_rider_active_password");
    }

    @Test
    void rejectsAnUnknownAccountStatus() {
        UUID id = riderRepository.saveAndFlush(withEmail("Laila Nour", "laila@gmail.com")).getId();

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE rider SET account_status = 'blocked' WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_rider_account_status");
    }

    // E15 case 13
    @Test
    void findsARiderByEmail() {
        Rider laila = riderRepository.saveAndFlush(withEmail("Laila Nour", "laila@gmail.com"));
        riderRepository.saveAndFlush(withEmail("Adam Samir", "adam@gmail.com"));

        assertThat(riderRepository.findByEmail("laila@gmail.com")).contains(laila);
        assertThat(riderRepository.findByEmail("nobody@gmail.com")).isEmpty();
        assertThat(riderRepository.existsByEmail("adam@gmail.com")).isTrue();
        assertThat(riderRepository.existsByEmail("nobody@gmail.com")).isFalse();
    }

    @Test
    void checksARidersAccountStatus() {
        Rider laila = withEmail("Laila Nour", "laila@gmail.com");
        laila.markInvited();
        laila.setPassword("$2a$10$hash");
        UUID lailaId = riderRepository.saveAndFlush(laila).getId();
        Rider adam = withEmail("Adam Samir", "adam@gmail.com");
        adam.markInvited();
        UUID adamId = riderRepository.saveAndFlush(adam).getId();

        assertThat(riderRepository.existsByIdAndAccountStatus(lailaId, RiderAccountStatus.ACTIVE)).isTrue();
        assertThat(riderRepository.existsByIdAndAccountStatus(adamId, RiderAccountStatus.ACTIVE)).isFalse();
        assertThat(riderRepository.existsByIdAndAccountStatus(adamId, RiderAccountStatus.INVITED)).isTrue();
        assertThat(riderRepository.existsByIdAndAccountStatus(UUID.randomUUID(), RiderAccountStatus.ACTIVE)).isFalse();
        assertThat(jdbcTemplate.queryForObject("SELECT account_status FROM rider WHERE id = ?", String.class, lailaId))
                .isEqualTo("active");
    }

    @Test
    void initialsUseFirstAndLastName() {
        assertThat(named("Sara Adel Hassan").initials()).isEqualTo("SH");
        assertThat(named("  laila   nour ").initials()).isEqualTo("LN");
        assertThat(named("Omar").initials()).isEqualTo("O");
        assertThat(named(" ").initials()).isEmpty();
        assertThat(new Rider().initials()).isEmpty();
    }

    private Rider rider(String name, short levelId, Trainer trainer) {
        Rider rider = named(name);
        rider.setLevel(levelRepository.getReferenceById(levelId));
        rider.setTrainer(trainer);
        return rider;
    }

    private Rider withEmail(String name, String email) {
        Rider rider = rider(name, LEVEL_1, sara);
        rider.setEmail(email);
        return rider;
    }

    private Rider named(String name) {
        Rider rider = new Rider();
        rider.setName(name);
        return rider;
    }

    private Trainer trainer(String name, String phone, String email) {
        Trainer trainer = new Trainer();
        trainer.setName(name);
        trainer.setPhone(phone);
        trainer.setEmail(email);
        return trainer;
    }
}
