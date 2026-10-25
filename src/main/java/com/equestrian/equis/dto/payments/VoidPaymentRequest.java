package com.equestrian.equis.dto.payments;

import com.equestrian.equis.common.error.ErrorCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// POST /admin/payments/{id}/void. The reason is kept in the payment history; 300 is the column's limit.
public record VoidPaymentRequest(
        @NotBlank(message = ErrorCodes.REASON_REQUIRED)
        @Size(max = 300, message = ErrorCodes.REASON_TOO_LONG)
        String reason) {
    public VoidPaymentRequest {
        reason = reason == null || reason.isBlank() ? null : reason.trim();
    }
}
