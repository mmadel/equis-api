package com.equestrian.equis.dto.assessment;

import java.util.UUID;

// PATCH /admin/bookings/{id}. At least one field; that is checked in AdminBookingChangeService.
// slotId on an upcoming booking reschedules it; on a no-show or cancelled booking it rebooks the member.
public record ChangeBookingRequest(UUID slotId, UUID assessorId) {}
