package com.equestrian.equis.service.platform;

// What a club uses against its plan's limits. A null max means unlimited, or no current plan.
public record Usage(int trainers, Integer maxTrainers, int riders, Integer maxRiders) {
}
