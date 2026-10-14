package com.equestrian.equis.entity.levels;

// The seeded level ids run from MIN_LEVEL to MAX_LEVEL (see V2__seed_levels.sql).
public final class Levels {
    private Levels() {}

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 6;

    public static boolean isValid(Integer id) {
        return id != null && id >= MIN_LEVEL && id <= MAX_LEVEL;
    }
}
