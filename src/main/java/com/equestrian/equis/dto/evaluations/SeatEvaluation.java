package com.equestrian.equis.dto.evaluations;

import com.equestrian.equis.entity.evaluations.SessionEvaluation;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

// The evaluation saved for one seat. comment is null when none was given.
public record SeatEvaluation(int stars,
                             @JsonInclude(JsonInclude.Include.ALWAYS) String comment,
                             Instant createdAt) {

    public static SeatEvaluation of(SessionEvaluation evaluation) {
        return new SeatEvaluation(evaluation.getStars(), evaluation.getComment(), evaluation.getCreatedAt());
    }
}
