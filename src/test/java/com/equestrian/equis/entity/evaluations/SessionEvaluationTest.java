package com.equestrian.equis.entity.evaluations;

import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// E14 cases 8–9.
class SessionEvaluationTest {
    private static final Instant NOW = Instant.parse("2026-11-03T16:10:00Z"); // 18:10 Cairo

    private final Session session = Session.of(new SessionTemplate(), LocalDate.of(2026, 11, 3));
    private final Rider sara = new Rider();
    private final Trainer ahmed = Trainer.invite("Ahmed Hassan", "+201000000001", "ahmed@example.com");

    // Case 8
    @Test
    void keepsTheStarsAndTheTrimmedComment() {
        SessionEvaluation evaluation = SessionEvaluation.of(session, sara, ahmed, 4, "  Better heels down ", NOW);

        assertThat(evaluation.getStars()).isEqualTo((short) 4);
        assertThat(evaluation.getComment()).isEqualTo("Better heels down");
        assertThat(evaluation.getSession()).isSameAs(session);
        assertThat(evaluation.getRider()).isSameAs(sara);
        assertThat(evaluation.getTrainer()).isSameAs(ahmed);
        assertThat(evaluation.getCreatedAt()).isEqualTo(NOW);
    }

    // Case 8
    @Test
    void aBlankCommentIsStoredAsNull() {
        assertThat(SessionEvaluation.of(session, sara, ahmed, 3, "   ", NOW).getComment()).isNull();
        assertThat(SessionEvaluation.of(session, sara, ahmed, 3, null, NOW).getComment()).isNull();
    }

    @Test
    void a500CharacterCommentIsAccepted() {
        String comment = "a".repeat(500);
        assertThat(SessionEvaluation.of(session, sara, ahmed, 5, " " + comment + " ", NOW).getComment())
                .isEqualTo(comment);
    }

    // Case 9
    @ParameterizedTest
    @ValueSource(ints = {0, 6})
    void rejectsStarsOutside1To5(int stars) {
        assertThatThrownBy(() -> SessionEvaluation.of(session, sara, ahmed, stars, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // Case 9
    @Test
    void rejectsACommentOver500Characters() {
        assertThatThrownBy(() -> SessionEvaluation.of(session, sara, ahmed, 4, "a".repeat(501), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
