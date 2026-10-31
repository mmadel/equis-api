package com.equestrian.equis.dto.evaluations;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

// GET /me/evaluations/pending, for the trainer's Home: count is the riders still to evaluate across sessions,
// which are the finished sessions with at least one of them, newest first.
public record PendingEvaluations(long count, List<PendingSession> sessions) {

    public record PendingSession(UUID sessionId,
                                 LocalDate date,
                                 @JsonFormat(pattern = "HH:mm") LocalTime start,
                                 @JsonFormat(pattern = "HH:mm") LocalTime end,
                                 int levelId,
                                 int ridersToEvaluate) {
    }
}
