package com.equestrian.equis.dto.overview;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

// A session running today. levelId is null for an assessment slot. trainers: the trainers of the riders in a level
// session, or the assessors of an assessment slot, sorted. state is "finished", "in_progress" or "upcoming".
public record TodaySession(UUID sessionId,
                           Integer levelId,
                           @JsonFormat(pattern = "HH:mm") LocalTime start,
                           @JsonFormat(pattern = "HH:mm") LocalTime end,
                           int capacity,
                           int booked,
                           List<String> trainers,
                           String state) {
}
