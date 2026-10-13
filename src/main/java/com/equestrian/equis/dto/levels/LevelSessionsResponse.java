package com.equestrian.equis.dto.levels;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

// Days without a session are left out.
public record LevelSessionsResponse(LevelRef level, LocalDate from, LocalDate to, List<Day> days) {

    public record Day(LocalDate date, List<SessionSlot> sessions) {
    }

    // past is true once the session has started.
    public record SessionSlot(UUID id,
                              @JsonFormat(pattern = "HH:mm") LocalTime start,
                              @JsonFormat(pattern = "HH:mm") LocalTime end,
                              int capacity,
                              int booked,
                              boolean past,
                              List<Seat> seats) {
    }

    // A seat given by another trainer carries no rider details.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Seat(boolean mine, UUID riderId, String initials) {
        public static Seat other() {
            return new Seat(false, null, null);
        }
    }
}
