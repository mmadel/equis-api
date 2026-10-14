package com.equestrian.equis.dto.assessment;

import com.equestrian.equis.entity.assessment.AssessmentBooking;

import java.util.UUID;

// The answer to registering a member: the new member and their booking.
public record BookingCreated(UUID bookingId, UUID memberId, UUID slotId) {
    // getId() on the member and session proxies reads the id without a query.
    public static BookingCreated of(AssessmentBooking b) {
        return new BookingCreated(b.getId(), b.getMember().getId(), b.getSession().getId());
    }
}
