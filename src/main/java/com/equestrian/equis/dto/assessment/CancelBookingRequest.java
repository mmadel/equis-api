package com.equestrian.equis.dto.assessment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// POST /admin/bookings/{id}/cancel. reason is a CancelReason code ("person_unwell", ...); reasonText only with
// "other", where it is required. Both rules are checked in AdminBookingCancelService.
public record CancelBookingRequest(@NotBlank String reason,
                                   @Size(max = 200) String reasonText) {
    public CancelBookingRequest {
        reason = reason == null ? null : reason.trim();
        reasonText = reasonText == null || reasonText.isBlank() ? null : reasonText.trim();
    }
}
