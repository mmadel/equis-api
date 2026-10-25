package com.equestrian.equis.dto.payments;

import com.equestrian.equis.common.error.ErrorCodes;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.YearMonth;

// POST /admin/riders/{id}/payments. method is a PaymentMethod code ("cash", ...), checked in AdminPaymentService
// along with the start month range. sessions left out means 8 per month.
public record RecordPaymentRequest(
        @NotNull(message = ErrorCodes.INVALID_AMOUNT)
        @DecimalMin(value = "0.01", message = ErrorCodes.INVALID_AMOUNT)
        @Digits(integer = 8, fraction = 2, message = ErrorCodes.INVALID_AMOUNT)
        BigDecimal amountEgp,
        @NotBlank(message = ErrorCodes.INVALID_METHOD) String method,
        @NotNull YearMonth startMonth,
        @NotNull @Min(1) @Max(12) Integer months,
        @Min(1) @Max(200) Integer sessions) {
    public RecordPaymentRequest {
        method = method == null ? null : method.trim();
    }
}
