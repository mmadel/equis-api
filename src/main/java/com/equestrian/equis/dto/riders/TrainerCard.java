package com.equestrian.equis.dto.riders;

import java.util.UUID;

// A trainer card on W3. status is "active" or "disabled"; riderCount covers every rider, whatever the filter.
public record TrainerCard(UUID id, String name, String status, long riderCount) {
}
