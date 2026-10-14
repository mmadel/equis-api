package com.equestrian.equis.dto.assessment;

import java.util.UUID;

// status is "upcoming", "waiting", "placed" or "noshow". levelId is set only when placed;
// memberPhone and notes only on the caller's own bookings. Nulls are sent, not left out.
public record BookingRowDto(UUID id,
                            UUID memberId,
                            String memberName,
                            String memberPhone,
                            UUID assessorId,
                            String assessorName,
                            boolean mine,
                            String status,
                            Integer levelId,
                            String notes) {
}
