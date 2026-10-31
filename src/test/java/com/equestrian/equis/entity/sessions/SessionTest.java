package com.equestrian.equis.entity.sessions;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// E14 cases 10–12: the Done state.
class SessionTest {
    private static final UUID AHMED = UUID.randomUUID();
    private static final Instant FINISHED_AT = Instant.parse("2026-11-03T16:05:00Z"); // 18:05 Cairo

    // Case 10
    @Test
    void becomesDoneWhenTheLastSeatedRiderIsEvaluated() {
        Session session = finished();

        assertThat(session.markDoneIfAllEvaluated(2, 1)).isFalse();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.FINISHED);

        assertThat(session.markDoneIfAllEvaluated(2, 2)).isTrue();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.DONE);
        assertThat(session.isDone()).isTrue();
        assertThat(session.getFinishedAt()).isEqualTo(FINISHED_AT);
        assertThat(session.getFinishedBy()).isEqualTo(AHMED);
    }

    // Case 11
    @Test
    void onlyAFinishedSessionCanBecomeDone() {
        Session scheduled = scheduled();
        Session cancelled = scheduled();
        cancelled.cancel(FINISHED_AT);
        Session done = done();

        assertThat(scheduled.markDoneIfAllEvaluated(0, 0)).isFalse();
        assertThat(cancelled.markDoneIfAllEvaluated(0, 0)).isFalse();
        assertThat(done.markDoneIfAllEvaluated(2, 2)).isFalse();

        assertThat(scheduled.getStatus()).isEqualTo(SessionStatus.SCHEDULED);
        assertThat(cancelled.getStatus()).isEqualTo(SessionStatus.CANCELLED);
        assertThat(done.getStatus()).isEqualTo(SessionStatus.DONE);
    }

    // Case 12
    @Test
    void aDoneSessionCountsAsFinished() {
        assertThat(scheduled().isFinished()).isFalse();
        assertThat(finished().isFinished()).isTrue();
        assertThat(done().isFinished()).isTrue();
        assertThat(finished().isDone()).isFalse();
    }

    // Case 12
    @Test
    void aDoneSessionCantBeCancelledOrFinishedAgain() {
        Session done = done();

        assertThatThrownBy(() -> done.cancel(FINISHED_AT)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> done.finish(AHMED, FINISHED_AT)).isInstanceOf(IllegalStateException.class);
        assertThat(done.getStatus()).isEqualTo(SessionStatus.DONE);
    }

    @Test
    void doneIsStoredLowercase() {
        assertThat(new SessionStatus.DbConverter().convertToDatabaseColumn(SessionStatus.DONE)).isEqualTo("done");
        assertThat(new SessionStatus.DbConverter().convertToEntityAttribute("done")).isEqualTo(SessionStatus.DONE);
    }

    private static Session scheduled() {
        SessionTemplate template = new SessionTemplate();
        template.setDayOfWeek(DayOfWeek.TUESDAY);
        template.setStartTime(LocalTime.of(17, 0));
        template.setEndTime(LocalTime.of(18, 0));
        template.setCapacity(6);
        return Session.of(template, LocalDate.of(2026, 11, 3));
    }

    private static Session finished() {
        Session session = scheduled();
        session.finish(AHMED, FINISHED_AT);
        return session;
    }

    private static Session done() {
        Session session = finished();
        session.markDoneIfAllEvaluated(0, 0);
        return session;
    }
}
