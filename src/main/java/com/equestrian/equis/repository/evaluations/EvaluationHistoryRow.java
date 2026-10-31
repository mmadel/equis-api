package com.equestrian.equis.repository.evaluations;

import com.equestrian.equis.dto.evaluations.RiderEvaluation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// One evaluation of a rider with its session and writer, in the column types JPQL gives back.
public record EvaluationHistoryRow(UUID sessionId, LocalDate date, LocalTime start, LocalTime end, Short levelId,
                                   String trainerName, Short stars, String comment, Instant createdAt) {

    public RiderEvaluation toDto() {
        return new RiderEvaluation(sessionId, date, start, end, levelId, trainerName, stars, comment, createdAt);
    }
}
