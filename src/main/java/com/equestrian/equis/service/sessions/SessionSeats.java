package com.equestrian.equis.service.sessions;

import com.equestrian.equis.dto.levels.LevelSessionsResponse.Seat;

import java.util.List;

// A level session's seats as one trainer sees them. mine counts the caller's seats; past is true once it has started.
public record SessionSeats(int capacity, int booked, int mine, boolean past, List<Seat> seats) {
}
