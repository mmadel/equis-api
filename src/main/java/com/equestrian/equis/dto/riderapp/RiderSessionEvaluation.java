package com.equestrian.equis.dto.riderapp;

import java.time.Instant;

// What the coach gave the rider for one ride. comment can be null.
public record RiderSessionEvaluation(int stars, String comment, Instant createdAt) {
}
