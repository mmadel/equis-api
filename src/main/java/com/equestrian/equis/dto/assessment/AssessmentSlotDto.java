package com.equestrian.equis.dto.assessment;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// booked counts every booking that isn't cancelled; open means not started and not full.
public record AssessmentSlotDto(UUID id,
                                LocalDate date,
                                @JsonFormat(pattern = "HH:mm") LocalTime start,
                                @JsonFormat(pattern = "HH:mm") LocalTime end,
                                int capacity,
                                long booked,
                                boolean open) {
}
