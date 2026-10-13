package com.equestrian.equis.dto.levels;

import com.equestrian.equis.entity.levels.Level;

public record LevelRef(int id, String name, String colour) {
    public static LevelRef of(Level level) {
        return new LevelRef(level.getId(), level.getName(), level.getColour());
    }
}
