package com.equestrian.equis.dto.riders;

import com.equestrian.equis.entity.sessions.Session;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// One seat in a dated session: the freed seats of a level change, the 4b preview and "Next" on screen 4.
public record UpcomingSeat(UUID sessionId, LocalDate date,
                           @JsonFormat(pattern = "HH:mm") LocalTime start,
                           @JsonFormat(pattern = "HH:mm") LocalTime end) {
    public static UpcomingSeat of(Session s) {
        return new UpcomingSeat(s.getId(), s.getSessionDate(), s.getStartTime(), s.getEndTime());
    }
}
