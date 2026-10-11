package com.equestrian.equis.repository.accounts;

import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
public class TrainerRepositoryTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void savesWithGeneratedIdDefaultStatusAndTimestamps() {
        Trainer saved = trainerRepository.saveAndFlush(trainer("Sara Ali", "01000000001", "sara@example.com"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(TrainerStatus.INVITED);
        assertThat(saved.getPasswordHash()).isNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void storesStatusAsLowercase() {
        Trainer saved = trainerRepository.saveAndFlush(trainer("Sara Ali", "01000000001", "sara@example.com"));

        String status = jdbcTemplate.queryForObject("SELECT status FROM trainer WHERE id = ?", String.class, saved.getId());
        assertThat(status).isEqualTo("invited");
    }

    @Test
    void readsStatusWrittenByTheDatabase() {
        insert("Omar Hassan", "01000000002", "omar@example.com", "hash", "disabled");

        assertThat(trainerRepository.findByEmail("omar@example.com").orElseThrow().getStatus())
                .isEqualTo(TrainerStatus.DISABLED);
    }

    @Test
    void findsByEmail() {
        Trainer saved = trainerRepository.saveAndFlush(trainer("Sara Ali", "01000000001", "sara@example.com"));

        assertThat(trainerRepository.findByEmail("sara@example.com")).contains(saved);
        assertThat(trainerRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void findsByPhone() {
        Trainer saved = trainerRepository.saveAndFlush(trainer("Sara Ali", "01000000001", "sara@example.com"));

        assertThat(trainerRepository.findByPhone("01000000001")).contains(saved);
        assertThat(trainerRepository.findByPhone("01999999999")).isEmpty();
    }

    @Test
    void existsByEmailAndPhone() {
        trainerRepository.saveAndFlush(trainer("Sara Ali", "01000000001", "sara@example.com"));

        assertThat(trainerRepository.existsByEmail("sara@example.com")).isTrue();
        assertThat(trainerRepository.existsByEmail("nobody@example.com")).isFalse();
        assertThat(trainerRepository.existsByPhone("01000000001")).isTrue();
        assertThat(trainerRepository.existsByPhone("01999999999")).isFalse();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> insert("   ", "01000000001", "sara@example.com", null, "invited"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_trainer_name");
    }

    @ParameterizedTest
    @ValueSource(strings = {"11000000001", "0100000000", "0100000000a"})
    void rejectsMalformedPhone(String phone) {
        assertThatThrownBy(() -> insert("Sara Ali", phone, "sara@example.com", null, "invited"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_trainer_phone");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Sara@example.com", "@example.com", "sara.example.com"})
    void rejectsMixedCaseOrMalformedEmail(String email) {
        assertThatThrownBy(() -> insert("Sara Ali", "01000000001", email, null, "invited"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_trainer_email");
    }

    @Test
    void rejectsUnknownStatus() {
        assertThatThrownBy(() -> insert("Sara Ali", "01000000001", "sara@example.com", null, "INVITED"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_trainer_status");
    }

    @Test
    void rejectsActiveTrainerWithoutPassword() {
        assertThatThrownBy(() -> insert("Sara Ali", "01000000001", "sara@example.com", null, "active"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_trainer_active_password");
    }

    @Test
    void rejectsDuplicatePhone() {
        insert("Sara Ali", "01000000001", "sara@example.com", null, "invited");
        assertThatThrownBy(() -> insert("Omar Hassan", "01000000001", "omar@example.com", null, "invited"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_trainer_phone");
    }

    @Test
    void rejectsDuplicateEmail() {
        insert("Sara Ali", "01000000001", "sara@example.com", null, "invited");
        assertThatThrownBy(() -> insert("Omar Hassan", "01000000002", "sara@example.com", null, "invited"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_trainer_email");
    }

    private Trainer trainer(String name, String phone, String email) {
        Trainer trainer = new Trainer();
        trainer.setName(name);
        trainer.setPhone(phone);
        trainer.setEmail(email);
        return trainer;
    }

    private void insert(String name, String phone, String email, String passwordHash, String status) {
        jdbcTemplate.update("INSERT INTO trainer (name, phone, email, password_hash, status) VALUES (?, ?, ?, ?, ?)",
                name, phone, email, passwordHash, status);
    }
}
