package com.equestrian.equis.service.platform;

// What a club may do right now. Worked out from the club and its current subscription, never stored
// (ClubAccessService.stateOf).
public enum AccessState {
    ACTIVE,
    // The club can look at its data but not change it.
    READ_ONLY,
    // The club can't be used.
    BLOCKED
}
