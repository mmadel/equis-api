package com.equestrian.equis.dto.sessions;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// A null levelId means an assessment slot.
// maxBookedInUpcoming is the fullest upcoming session; bookingsInUpcoming is the total across all of them.
public record SessionTemplateResponse(UUID id,
                                      Integer levelId,
                                      DayOfWeek dayOfWeek,
                                      @JsonFormat(pattern = "HH:mm") LocalTime start,
                                      @JsonFormat(pattern = "HH:mm") LocalTime end,
                                      int capacity,
                                      LocalDate validFrom,
                                      int maxBookedInUpcoming,
                                      int bookingsInUpcoming) {
}
