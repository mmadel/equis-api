package com.equestrian.equis.dto.sessions;

import com.equestrian.equis.dto.levels.LevelRef;
import com.equestrian.equis.dto.levels.LevelSessionsResponse.Seat;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

// The school week from Saturday to Friday, every level side by side. days always has seven items, Saturday first;
// a day without a session has no sessions. levels is the colour legend, ordered by id.
public record MyWeek(LocalDate from, LocalDate to, List<LevelRef> levels, List<WeekDay> days) {

    public record WeekDay(LocalDate date, List<WeekSession> sessions) {
    }

    // mine counts the caller's riders in the session; past is true once it has started.
    public record WeekSession(UUID id,
                              int levelId,
                              @JsonFormat(pattern = "HH:mm") LocalTime start,
                              @JsonFormat(pattern = "HH:mm") LocalTime end,
                              int capacity,
                              int booked,
                              int mine,
                              boolean past,
                              List<Seat> seats) {
    }
}
