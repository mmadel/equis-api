package com.equestrian.equis.dto.levels;

// One level as the signed-in trainer sees it. The week is the school's current week, Saturday to Friday.
// seatsFilled counts every trainer's riders; myRiders only the caller's.
public record MyLevelSummary(int id,
                             String name,
                             String colour,
                             long sessionsThisWeek,
                             long myRiders,
                             long seatsFilled,
                             long seatsTotal) {
}
