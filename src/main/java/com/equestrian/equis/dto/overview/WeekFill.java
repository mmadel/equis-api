package com.equestrian.equis.dto.overview;

import java.time.LocalDate;
import java.util.List;

// Seats taken and offered in the school week from..to. byTrack always has seven entries: Level 1–6, then assessment.
public record WeekFill(LocalDate from, LocalDate to, int booked, int capacity, List<TrackFill> byTrack) {
}
