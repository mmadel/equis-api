package com.equestrian.equis.dto.evaluations;

import java.util.List;
import java.util.UUID;

// GET /me/riders/{id}/evaluations and GET /admin/riders/{id}/evaluations: every evaluation of the rider,
// newest session first. The assessment evaluation is not part of it.
public record RiderEvaluations(UUID riderId, String riderName, List<RiderEvaluation> evaluations) {
}
