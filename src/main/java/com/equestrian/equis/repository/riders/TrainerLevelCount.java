package com.equestrian.equis.repository.riders;

import java.util.UUID;

// How many of one trainer's riders are in one level.
public interface TrainerLevelCount {
    UUID getTrainerId();

    Short getLevelId();

    long getRiderCount();
}
