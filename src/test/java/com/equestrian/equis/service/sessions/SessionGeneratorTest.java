package com.equestrian.equis.service.sessions;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.common.config.SessionProperties;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionStatus;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableConfigurationProperties(SessionProperties.class)
@Import(SessionGenerator.class)
public class SessionGeneratorTest {
    private static final short LEVEL_1 = 1;
    // app.sessions.horizon-weeks
    private static final int HORIZON_WEEKS = 8;
    // The clock is fixed on this Monday, so the horizon ends on Sunday 2026-11-29.
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @Autowired
    SessionGenerator sessionGenerator;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    LevelRepository levelRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void generatesOneSessionPerWeekFromTodayToTheEndOfTheHorizon() {
        SessionTemplate template = template(DayOfWeek.MONDAY);

        assertThat(sessionGenerator.generateFor(template)).isEqualTo(HORIZON_WEEKS);

        assertThat(datesOf(template)).containsExactly(
                TODAY, TODAY.plusWeeks(1), TODAY.plusWeeks(2), TODAY.plusWeeks(3),
                TODAY.plusWeeks(4), TODAY.plusWeeks(5), TODAY.plusWeeks(6), TODAY.plusWeeks(7));
    }

    @Test
    void lastDayOfTheHorizonIsIncludedAndTheDayAfterIsNot() {
        SessionTemplate sunday = template(DayOfWeek.SUNDAY);

        sessionGenerator.generateFor(sunday);

        List<LocalDate> dates = datesOf(sunday);
        assertThat(dates).hasSize(HORIZON_WEEKS);
        assertThat(dates.getFirst()).isEqualTo(LocalDate.of(2026, 10, 11));
        assertThat(dates.getLast()).isEqualTo(LocalDate.of(2026, 11, 29));
    }

    @Test
    void copiesTheTemplateTimesAndCapacity() {
        SessionTemplate template = template(DayOfWeek.MONDAY);

        sessionGenerator.generateFor(template);

        Session session = sessionRepository.findAll().stream()
                .filter(s -> s.getTemplate().equals(template) && s.getSessionDate().equals(TODAY))
                .findFirst().orElseThrow();
        assertThat(session.getStartTime()).isEqualTo(LocalTime.of(16, 0));
        assertThat(session.getEndTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(session.getCapacity()).isEqualTo(6);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.SCHEDULED);
    }

    @Test
    void runningAgainAddsNothing() {
        SessionTemplate template = template(DayOfWeek.MONDAY);
        sessionGenerator.generateFor(template);

        assertThat(sessionGenerator.generateFor(template)).isZero();

        assertThat(datesOf(template)).hasSize(HORIZON_WEEKS);
    }

    @Test
    void fillsOnlyTheMissingDates() {
        SessionTemplate template = template(DayOfWeek.MONDAY);
        sessionRepository.saveAndFlush(Session.of(template, TODAY.plusWeeks(1)));

        assertThat(sessionGenerator.generateFor(template)).isEqualTo(HORIZON_WEEKS - 1);
    }

    @Test
    void startsAtValidFromWhenItIsInTheFuture() {
        SessionTemplate template = template(DayOfWeek.MONDAY);
        // A Tuesday, so the first session is the Monday after.
        template.setValidFrom(LocalDate.of(2026, 10, 20));
        sessionTemplateRepository.saveAndFlush(template);

        assertThat(sessionGenerator.generateFor(template)).isEqualTo(5);

        assertThat(datesOf(template).getFirst()).isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    void stopsAtValidTo() {
        SessionTemplate template = template(DayOfWeek.MONDAY);
        template.setValidTo(TODAY.plusWeeks(2));
        sessionTemplateRepository.saveAndFlush(template);

        assertThat(sessionGenerator.generateFor(template)).isEqualTo(3);

        assertThat(datesOf(template)).containsExactly(TODAY, TODAY.plusWeeks(1), TODAY.plusWeeks(2));
    }

    @Test
    void generatesNothingWhenValidFromIsPastTheHorizon() {
        SessionTemplate template = template(DayOfWeek.MONDAY);
        template.setValidFrom(TODAY.plusWeeks(HORIZON_WEEKS));
        sessionTemplateRepository.saveAndFlush(template);

        assertThat(sessionGenerator.generateFor(template)).isZero();
    }

    @Test
    void skipsADeletedTemplate() {
        SessionTemplate template = template(DayOfWeek.MONDAY);
        template.setDeletedAt(Instant.now());
        sessionTemplateRepository.saveAndFlush(template);

        assertThat(sessionGenerator.generateFor(template)).isZero();

        assertThat(datesOf(template)).isEmpty();
    }

    // Runs outside the usual test transaction because generateAll commits each template on its own.
    // Uses the five templates seeded by V8, all valid from 2026-10-01.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void generateAllCoversEveryActiveTemplate() {
        try {
            assertThat(sessionGenerator.generateAll()).isEqualTo(new GenerationResult(5 * HORIZON_WEEKS, 5));
            assertThat(sessionGenerator.generateAll()).isEqualTo(new GenerationResult(0, 5));
        } finally {
            jdbcTemplate.update("DELETE FROM session");
        }
    }

    private List<LocalDate> datesOf(SessionTemplate template) {
        return jdbcTemplate.queryForList("SELECT session_date FROM session WHERE template_id = ? ORDER BY session_date",
                LocalDate.class, template.getId());
    }

    private SessionTemplate template(DayOfWeek day) {
        SessionTemplate template = new SessionTemplate();
        template.setLevel(levelRepository.getReferenceById(LEVEL_1));
        template.setDayOfWeek(day);
        template.setStartTime(LocalTime.of(16, 0));
        template.setEndTime(LocalTime.of(17, 0));
        template.setCapacity(6);
        template.setValidFrom(LocalDate.of(2026, 10, 1));
        return sessionTemplateRepository.saveAndFlush(template);
    }

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-05T10:00:00Z"), ZoneOffset.UTC);
        }
    }
}
