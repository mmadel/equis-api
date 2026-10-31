package com.equestrian.equis.dto.payments;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// One session taken from a package. trainerName is the coach who seated the rider in that session, null if that
// seat is gone.
public record PaymentUsage(UUID sessionId,
                           LocalDate date,
                           @JsonFormat(pattern = "HH:mm") LocalTime start,
                           @JsonFormat(pattern = "HH:mm") LocalTime end,
                           Integer levelId,
                           String trainerName,
                           Instant usedAt) {
}
