package com.equestrian.equis.repository.riders;

// How many of a trainer's riders are in one level.
public interface LevelRiderCount {
    Short getLevelId();

    long getRiderCount();
}
