package com.equestrian.equis.dto.overview;

import java.util.UUID;

// A trainer and how many riders they still have.
public record TrainerRiderCount(UUID id, String name, long riderCount) {
}
