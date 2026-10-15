package com.equestrian.equis.service.riders;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.riders.Direction;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.levels.Levels;
import com.equestrian.equis.entity.riders.Rider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Checks on the locked rider. The service calls them in this order, so a request that fails two ways always gets the same error.
@Component
public class LevelChangeRules {

    // Missing, unassigned and other trainers' riders look the same, so a trainer cannot learn that a rider exists.
    public void requireOwnRider(Rider rider, UUID trainerId) {
        Trainer trainer = rider.getTrainer();
        if (trainer == null || !trainer.getId().equals(trainerId)) {
            throw riderNotFound();
        }
    }

    public void requireExpectedLevel(Rider rider, int fromLevelId) {
        if (rider.getLevel().getId() != fromLevelId) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.LEVEL_CHANGED,
                    "This rider's level has changed. Reload and try again.");
        }
    }

    // The level one step away, or a conflict at either end of the ladder.
    public int targetLevel(Rider rider, Direction direction) {
        int from = rider.getLevel().getId();
        int to = direction == Direction.UP ? from + 1 : from - 1;
        if (to > Levels.MAX_LEVEL) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.LEVEL_AT_TOP,
                    "Level " + from + " is the top level.");
        }
        if (to < Levels.MIN_LEVEL) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.LEVEL_AT_BOTTOM,
                    "Level " + from + " is the first level.");
        }
        return to;
    }

    public ApiException riderNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.RIDER_NOT_FOUND, "This rider does not exist.");
    }
}
