package com.equestrian.equis.service.club;

// What a plan limits. Sent as the resource of a plan_limit_reached error.
public enum LimitedResource {
    // Trainers who aren't disabled (invited and active).
    TRAINERS,
    // People (member) who aren't closed: waiting for an assessment, no-show and riders.
    RIDERS
}
