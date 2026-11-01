package com.equestrian.equis.repository.payments;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// One ride of a rider with its evaluation, flat, in the column types JPQL gives back. stars, comment and
// evaluatedAt are null when the ride isn't evaluated.
public record RiderSessionRow(UUID sessionId, LocalDate date, LocalTime start, LocalTime end, Short levelId,
                              String trainerName, Short stars, String comment, Instant evaluatedAt) {
}
