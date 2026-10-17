package com.equestrian.equis.common.time;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

// The school week runs Saturday to Friday.
public final class SchoolWeek {
    public static final DayOfWeek START = DayOfWeek.SATURDAY;
    public static final int DAYS = 7;

    private SchoolWeek() {
    }

    // The Saturday that starts the school week containing date.
    public static LocalDate saturdayOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(START));
    }

    // The Friday that ends the school week starting on saturday.
    public static LocalDate fridayOf(LocalDate saturday) {
        return saturday.plusDays(DAYS - 1);
    }
}
