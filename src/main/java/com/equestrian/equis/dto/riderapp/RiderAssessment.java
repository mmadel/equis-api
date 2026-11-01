package com.equestrian.equis.dto.riderapp;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

// The assessment the rider was placed at: the slot, the assessor, the level given and their notes (can be null).
public record RiderAssessment(LocalDate date,
                              @JsonFormat(pattern = "HH:mm") LocalTime start,
                              @JsonFormat(pattern = "HH:mm") LocalTime end,
                              String assessorName, int levelId, String notes) {
}
