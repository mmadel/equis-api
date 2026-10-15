package com.equestrian.equis.dto.riders;

import com.equestrian.equis.entity.levels.Levels;
import com.equestrian.equis.entity.riders.LevelChange;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// fromLevelId is the level the confirm sheet showed; a rider no longer on it gets level_changed,
// so a double tap moves the rider one step only.
public record LevelChangeRequest(@NotNull Direction direction,
                                 @NotNull @Min(Levels.MIN_LEVEL) @Max(Levels.MAX_LEVEL) Integer fromLevelId,
                                 @NotBlank @Size(max = LevelChange.REASON_MAX_LENGTH) String reason) {
    public LevelChangeRequest {
        reason = reason == null ? null : reason.trim();
    }
}
