package com.equestrian.equis.common.time;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class DateRanges {
    private DateRanges() {
    }

    // from..to, both included, must be in order and cover at most maxDays days.
    public static void require(LocalDate from, LocalDate to, int maxDays) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) >= maxDays) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_RANGE,
                    "The dates must be in order and cover at most " + maxDays + " days.");
        }
    }
}
