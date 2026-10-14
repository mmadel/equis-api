package com.equestrian.equis.dto.assessment;

import java.util.UUID;

// The answer to recording a result. status is "placed" or "noshow"; riderId is set only when placed.
public record ResultRecorded(UUID bookingId, String status, UUID riderId) {
}
