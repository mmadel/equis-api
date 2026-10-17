package com.equestrian.equis.dto.overview;

// One level's week, or the assessment slots' when levelId is null.
public record TrackFill(Integer levelId, int booked, int capacity) {
}
