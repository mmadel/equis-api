package com.equestrian.equis.repository.evaluations;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// One finished session with riders the trainer seated and has not evaluated yet.
public record PendingEvaluationRow(UUID sessionId, LocalDate date, LocalTime start, LocalTime end, Short levelId,
                                   Long ridersToEvaluate) {
}
