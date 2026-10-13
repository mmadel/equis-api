package com.equestrian.equis.service.sessions;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

// What the rules check. Separate from the HTTP DTO, so the rules do not depend on the web layer.
// A null levelId means an assessment slot.
public record SessionTemplateInput(Integer levelId, DayOfWeek dayOfWeek, LocalTime start, LocalTime end,
                                   int capacity, LocalDate validFrom) {
}
