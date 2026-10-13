package com.equestrian.equis.repository.sessions;

// The scheduled sessions of one level in a date range, and the seats they offer in total.
public interface LevelSessionCount {
    Short getLevelId();

    long getSessionCount();

    long getSeatsTotal();
}
