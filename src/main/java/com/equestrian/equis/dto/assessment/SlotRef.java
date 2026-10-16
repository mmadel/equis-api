package com.equestrian.equis.dto.assessment;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// The slot's local date and school-time start and end.
public record SlotRef(UUID id,
                      LocalDate date,
                      @JsonFormat(pattern = "HH:mm") LocalTime start,
                      @JsonFormat(pattern = "HH:mm") LocalTime end) {
}
