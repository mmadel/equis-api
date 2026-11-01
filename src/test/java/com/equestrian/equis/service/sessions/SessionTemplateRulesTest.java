package com.equestrian.equis.service.sessions;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SessionTemplateRules.class)
public class SessionTemplateRulesTest {
    private static final int LEVEL_1 = 1;
    private static final int LEVEL_2 = 2;
    private static final int NO_SUCH_LEVEL = 999;
    private static final LocalDate VALID_FROM = LocalDate.of(2026, 10, 1);

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @Autowired
    SessionTemplateRules rules;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    LevelRepository levelRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @MockitoBean
    BookedSeats bookedSeats;

    // V8 seeds sample templates; clear them so each test starts from an empty table (rolled back afterwards).
    @BeforeEach
    void clearSeededTemplates() {
        jdbcTemplate.update("DELETE FROM session_template");
    }

    @Test
    void validInputWithNothingElseOnTheDayHasNoWarnings() {
        assertThat(rules.check(input(LEVEL_1, "16:00", "17:00", 6), null)).isEmpty();
    }

    @Test
    void assessmentSlotNeedsNoLevel() {
        assertThat(rules.check(input(null, "12:00", "13:00", 6), null)).isEmpty();
    }

    @Test
    void unknownLevelIsRefused() {
        assertInvalid(input(NO_SUCH_LEVEL, "16:00", "17:00", 6), null, "levelId");
        assertInvalid(input(100_000, "16:00", "17:00", 6), null, "levelId");
    }

    @Test
    void endMustBeAfterStart() {
        assertInvalid(input(LEVEL_1, "17:00", "16:00", 6), null, "end");
        assertInvalid(input(LEVEL_1, "16:00", "16:00", 6), null, "end");
    }

    @Test
    void capacityMustBeWithinRange() {
        assertInvalid(input(LEVEL_1, "16:00", "17:00", 0), null, "capacity");
        assertInvalid(input(LEVEL_1, "16:00", "17:00", 13), null, "capacity");

        assertThat(rules.check(input(LEVEL_1, "16:00", "17:00", 1), null)).isEmpty();
        assertThat(rules.check(input(LEVEL_1, "16:00", "17:00", 12), null)).isEmpty();
    }

    @Test
    void firstFailingRuleWins() {
        assertInvalid(input(NO_SUCH_LEVEL, "17:00", "16:00", 0), null, "levelId");
        assertInvalid(input(LEVEL_1, "17:00", "16:00", 0), null, "end");
    }

    @Test
    void editCannotDropCapacityBelowTheSeatsAlreadyBooked() {
        SessionTemplate existing = saved(LEVEL_1, DayOfWeek.MONDAY, "16:00", "17:00");
        when(bookedSeats.maxBookedInUpcoming(existing.getId())).thenReturn(5);

        assertThatThrownBy(() -> rules.check(input(LEVEL_1, "16:00", "17:00", 4), existing))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.code()).isEqualTo(ErrorCodes.CAPACITY_EXCEEDED);
                    assertThat(ex.properties()).containsEntry("booked", 5);
                });
    }

    @Test
    void editCanDropCapacityDownToTheSeatsAlreadyBooked() {
        SessionTemplate existing = saved(LEVEL_1, DayOfWeek.MONDAY, "16:00", "17:00");
        when(bookedSeats.maxBookedInUpcoming(existing.getId())).thenReturn(5);

        assertThat(rules.check(input(LEVEL_1, "16:00", "17:00", 5), existing)).isEmpty();
    }

    @Test
    void createDoesNotAskForBookedSeats() {
        rules.check(input(LEVEL_1, "16:00", "17:00", 6), null);

        verifyNoInteractions(bookedSeats);
    }

    @Test
    void overlappingTemplatesOfTheSameLevelAndDayComeBackAsOneWarning() {
        SessionTemplate early = saved(LEVEL_1, DayOfWeek.MONDAY, "15:30", "16:30");
        SessionTemplate late = saved(LEVEL_1, DayOfWeek.MONDAY, "16:30", "17:30");
        saved(LEVEL_1, DayOfWeek.MONDAY, "17:00", "18:00");

        List<Warning> warnings = rules.check(input(LEVEL_1, "16:00", "17:00", 6), null);

        assertThat(warnings).containsExactly(Warning.overlap(List.of(
                new OverlapRef(early.getId(), LocalTime.parse("15:30"), LocalTime.parse("16:30")),
                new OverlapRef(late.getId(), LocalTime.parse("16:30"), LocalTime.parse("17:30")))));
        assertThat(warnings.getFirst().code()).isEqualTo(Warning.OVERLAP);
    }

    @Test
    void otherLevelOtherDayAndDeletedTemplatesDoNotOverlap() {
        saved(LEVEL_2, DayOfWeek.MONDAY, "16:00", "17:00");
        saved(null, DayOfWeek.MONDAY, "16:00", "17:00");
        saved(LEVEL_1, DayOfWeek.TUESDAY, "16:00", "17:00");
        SessionTemplate deleted = template(LEVEL_1, DayOfWeek.MONDAY, "16:00", "17:00");
        deleted.setDeletedAt(Instant.now());
        sessionTemplateRepository.saveAndFlush(deleted);

        assertThat(rules.check(input(LEVEL_1, "16:00", "17:00", 6), null)).isEmpty();
    }

    @Test
    void assessmentSlotsOverlapOnlyWithOtherAssessmentSlots() {
        SessionTemplate assessment = saved(null, DayOfWeek.MONDAY, "16:00", "17:00");
        saved(LEVEL_1, DayOfWeek.MONDAY, "16:00", "17:00");

        assertThat(rules.check(input(null, "16:30", "17:30", 6), null)).containsExactly(Warning.overlap(List.of(
                new OverlapRef(assessment.getId(), LocalTime.parse("16:00"), LocalTime.parse("17:00")))));
    }

    @Test
    void templateThatEndedBeforeValidFromDoesNotOverlap() {
        SessionTemplate ended = template(LEVEL_1, DayOfWeek.MONDAY, "16:00", "17:00");
        ended.setValidFrom(VALID_FROM.minusMonths(2));
        ended.setValidTo(VALID_FROM.minusDays(1));
        sessionTemplateRepository.saveAndFlush(ended);

        assertThat(rules.check(input(LEVEL_1, "16:00", "17:00", 6), null)).isEmpty();
    }

    @Test
    void editedTemplateDoesNotOverlapWithItself() {
        SessionTemplate existing = saved(LEVEL_1, DayOfWeek.MONDAY, "16:00", "17:00");

        assertThat(rules.check(input(LEVEL_1, "16:30", "17:30", 6), existing)).isEmpty();
    }

    private void assertInvalid(SessionTemplateInput in, SessionTemplate existing, String field) {
        assertThatThrownBy(() -> rules.check(in, existing))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.code()).isEqualTo(ErrorCodes.VALIDATION_FAILED);
                    assertThat(ex.properties().get("fieldErrors"))
                            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.list(ApiFieldError.class))
                            .extracting(ApiFieldError::field)
                            .containsExactly(field);
                });
    }

    private SessionTemplateInput input(Integer levelId, String start, String end, int capacity) {
        return new SessionTemplateInput(levelId, DayOfWeek.MONDAY, LocalTime.parse(start), LocalTime.parse(end),
                capacity, VALID_FROM);
    }

    private SessionTemplate saved(Integer levelId, DayOfWeek day, String start, String end) {
        return sessionTemplateRepository.saveAndFlush(template(levelId, day, start, end));
    }

    private SessionTemplate template(Integer levelId, DayOfWeek day, String start, String end) {
        SessionTemplate template = new SessionTemplate();
        template.setLevel(levelId == null ? null : levelRepository.getReferenceById(levelId.shortValue()));
        template.setDayOfWeek(day);
        template.setStartTime(LocalTime.parse(start));
        template.setEndTime(LocalTime.parse(end));
        template.setCapacity(6);
        template.setValidFrom(VALID_FROM);
        return template;
    }
}
