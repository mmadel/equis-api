package com.equestrian.equis.repository.sessions;

// How many seats are filled in one level's scheduled sessions in a date range.
public interface LevelSeatCount {
    Short getLevelId();

    long getSeatCount();
}
