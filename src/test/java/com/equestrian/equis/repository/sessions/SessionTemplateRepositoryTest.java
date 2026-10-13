package com.equestrian.equis.repository.sessions;

import com.equestrian.equis.entity.levels.Level;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.levels.LevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class SessionTemplateRepositoryTest {
    private static final short LEVEL_1 = 1;
    private static final short LEVEL_2 = 2;

    // 2026-10-05 is a Monday.
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    LevelRepository levelRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    // V8 seeds sample templates; clear them so each test starts from an empty table (rolled back afterwards).
    @BeforeEach
    void clearSeededTemplates() {
        jdbcTemplate.update("DELETE FROM session_template");
    }

    @Test
    void savesWithGeneratedIdAndTimestamps() {
        SessionTemplate saved = sessionTemplateRepository.saveAndFlush(template(LEVEL_1, DayOfWeek.MONDAY, "16:00"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getDeletedAt()).isNull();
    }

    @Test
    void storesDayOfWeekAsIsoNumber() {
        SessionTemplate saved = sessionTemplateRepository.saveAndFlush(template(LEVEL_1, DayOfWeek.SUNDAY, "16:00"));

        Integer day = jdbcTemplate.queryForObject("SELECT day_of_week FROM session_template WHERE id = ?", Integer.class, saved.getId());
        assertThat(day).isEqualTo(7);
    }

    @Test
    void findsActiveTemplatesOfALevelOrderedByDayThenStart() {
        SessionTemplate saturday = sessionTemplateRepository.saveAndFlush(template(LEVEL_1, DayOfWeek.SATURDAY, "07:00"));
        SessionTemplate mondayLate = sessionTemplateRepository.saveAndFlush(template(LEVEL_1, DayOfWeek.MONDAY, "16:00"));
        SessionTemplate mondayEarly = sessionTemplateRepository.saveAndFlush(template(LEVEL_1, DayOfWeek.MONDAY, "09:00"));
        sessionTemplateRepository.saveAndFlush(template(LEVEL_2, DayOfWeek.MONDAY, "09:00"));
        sessionTemplateRepository.saveAndFlush(template(null, DayOfWeek.MONDAY, "12:00"));
        sessionTemplateRepository.saveAndFlush(deleted(template(LEVEL_1, DayOfWeek.TUESDAY, "09:00")));

        assertThat(sessionTemplateRepository.findByLevelIdAndDeletedAtIsNullOrderByDayOfWeekAscStartTimeAsc(LEVEL_1))
                .containsExactly(mondayEarly, mondayLate, saturday);
    }

    @Test
    void findsActiveAssessmentTemplatesOrderedByDayThenStart() {
        SessionTemplate saturday = sessionTemplateRepository.saveAndFlush(template(null, DayOfWeek.SATURDAY, "12:00"));
        SessionTemplate tuesday = sessionTemplateRepository.saveAndFlush(template(null, DayOfWeek.TUESDAY, "12:00"));
        sessionTemplateRepository.saveAndFlush(template(LEVEL_1, DayOfWeek.MONDAY, "09:00"));
        sessionTemplateRepository.saveAndFlush(deleted(template(null, DayOfWeek.MONDAY, "12:00")));

        assertThat(sessionTemplateRepository.findByLevelIsNullAndDeletedAtIsNullOrderByDayOfWeekAscStartTimeAsc())
                .containsExactly(tuesday, saturday);
    }

    @Test
    void findsAllActiveTemplates() {
        SessionTemplate level = sessionTemplateRepository.saveAndFlush(template(LEVEL_1, DayOfWeek.MONDAY, "09:00"));
        SessionTemplate assessment = sessionTemplateRepository.saveAndFlush(template(null, DayOfWeek.TUESDAY, "12:00"));
        sessionTemplateRepository.saveAndFlush(deleted(template(LEVEL_2, DayOfWeek.MONDAY, "09:00")));

        assertThat(sessionTemplateRepository.findByDeletedAtIsNull()).containsExactlyInAnyOrder(level, assessment);
    }

    @Test
    void findsByIdOnlyWhileNotDeleted() {
        SessionTemplate active = sessionTemplateRepository.saveAndFlush(template(LEVEL_1, DayOfWeek.MONDAY, "09:00"));
        SessionTemplate gone = sessionTemplateRepository.saveAndFlush(deleted(template(LEVEL_1, DayOfWeek.TUESDAY, "09:00")));

        assertThat(sessionTemplateRepository.findByIdAndDeletedAtIsNull(active.getId())).contains(active);
        assertThat(sessionTemplateRepository.findByIdAndDeletedAtIsNull(gone.getId())).isEmpty();
        assertThat(sessionTemplateRepository.findByIdAndDeletedAtIsNull(UUID.randomUUID())).isEmpty();
    }

    @Test
    void assessmentIsATemplateWithoutLevel() {
        assertThat(template(null, DayOfWeek.MONDAY, "12:00").isAssessment()).isTrue();
        assertThat(template(LEVEL_1, DayOfWeek.MONDAY, "12:00").isAssessment()).isFalse();
    }

    @Test
    void runsOnMatchingDayWithinValidity() {
        SessionTemplate template = template(LEVEL_1, DayOfWeek.MONDAY, "16:00");
        template.setValidFrom(MONDAY);
        template.setValidTo(MONDAY.plusWeeks(2));

        assertThat(template.runsOn(MONDAY)).isTrue();
        assertThat(template.runsOn(MONDAY.plusWeeks(2))).isTrue();
        assertThat(template.runsOn(MONDAY.plusDays(1))).isFalse();
        assertThat(template.runsOn(MONDAY.minusWeeks(1))).isFalse();
        assertThat(template.runsOn(MONDAY.plusWeeks(3))).isFalse();
    }

    @Test
    void runsOnHasNoEndWhenValidToIsNull() {
        SessionTemplate template = template(LEVEL_1, DayOfWeek.MONDAY, "16:00");
        template.setValidFrom(MONDAY);

        assertThat(template.runsOn(MONDAY.plusYears(5).with(DayOfWeek.MONDAY))).isTrue();
    }

    @Test
    void deletedTemplateNeverRuns() {
        SessionTemplate template = deleted(template(LEVEL_1, DayOfWeek.MONDAY, "16:00"));
        template.setValidFrom(MONDAY);

        assertThat(template.isDeleted()).isTrue();
        assertThat(template.runsOn(MONDAY)).isFalse();
    }

    private SessionTemplate template(Short levelId, DayOfWeek day, String start) {
        Level level = levelId == null ? null : levelRepository.getReferenceById(levelId);
        LocalTime startTime = LocalTime.parse(start);
        SessionTemplate template = new SessionTemplate();
        template.setLevel(level);
        template.setDayOfWeek(day);
        template.setStartTime(startTime);
        template.setEndTime(startTime.plusHours(1));
        template.setCapacity(6);
        template.setValidFrom(LocalDate.of(2026, 10, 1));
        return template;
    }

    private SessionTemplate deleted(SessionTemplate template) {
        template.setDeletedAt(Instant.now());
        return template;
    }
}
