package com.equestrian.equis.dto.evaluations;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// One line of a rider's evaluation history. levelId is the session's level, not the rider's current one;
// trainerName is the coach who wrote it. comment is null when none was given.
public record RiderEvaluation(UUID sessionId,
                              LocalDate date,
                              @JsonFormat(pattern = "HH:mm") LocalTime start,
                              @JsonFormat(pattern = "HH:mm") LocalTime end,
                              int levelId,
                              String trainerName,
                              int stars,
                              @JsonInclude(JsonInclude.Include.ALWAYS) String comment,
                              Instant createdAt) {
}
