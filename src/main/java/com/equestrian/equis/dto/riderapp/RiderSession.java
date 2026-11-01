package com.equestrian.equis.dto.riderapp;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// One ride. trainerName is who evaluated it, or who finished the session if it isn't evaluated yet.
// evaluation is null until the coach evaluates the rider.
public record RiderSession(UUID sessionId, LocalDate date,
                           @JsonFormat(pattern = "HH:mm") LocalTime start,
                           @JsonFormat(pattern = "HH:mm") LocalTime end,
                           int levelId, String trainerName, RiderSessionEvaluation evaluation) {
}
