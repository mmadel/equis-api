package com.equestrian.equis.dto.sessions;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

// The full template as it should be after the edit. A null levelId means an assessment slot.
public record SessionTemplateUpdateRequest(@Min(1) @Max(6) Integer levelId,
                                           @NotNull DayOfWeek dayOfWeek,
                                           @NotNull @JsonFormat(pattern = "HH:mm") LocalTime start,
                                           @NotNull @JsonFormat(pattern = "HH:mm") LocalTime end,
                                           @NotNull @Min(1) @Max(12) Integer capacity,
                                           @NotNull LocalDate validFrom) {
}
