package com.equestrian.equis.dto.evaluations;

import com.equestrian.equis.entity.evaluations.SessionEvaluation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

// POST /me/sessions/{id}/evaluations: one rider at a time. The comment is trimmed first; blank means none.
public record EvaluationRequest(
        @NotNull UUID riderId,
        @NotNull @Min(SessionEvaluation.MIN_STARS) @Max(SessionEvaluation.MAX_STARS) Integer stars,
        @Size(max = SessionEvaluation.MAX_COMMENT) String comment) {
    public EvaluationRequest {
        comment = comment == null || comment.isBlank() ? null : comment.trim();
    }
}
