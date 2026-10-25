package com.equestrian.equis.service.payments;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.payments.BalanceDto;
import com.equestrian.equis.dto.payments.PaymentResult;
import com.equestrian.equis.dto.payments.RecordPaymentRequest;
import com.equestrian.equis.dto.payments.RiderPayments;
import com.equestrian.equis.dto.payments.VoidPaymentRequest;
import com.equestrian.equis.entity.payments.Payment;
import com.equestrian.equis.entity.payments.PaymentMethod;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.repository.payments.PaymentRepository;
import com.equestrian.equis.repository.payments.SessionUsageRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// W3's Record payment: the admin saves what a rider paid at the desk as a new package. Any existing rider can be
// paid for, with or without a trainer. No lock: two payments at once are both valid and simply add up.
@Service
public class AdminPaymentService {
    private final PaymentRepository payments;
    private final SessionUsageRepository usages;
    private final RiderRepository riders;
    private final RiderBalanceService balances;
    private final PaymentEntryAssembler assembler;
    private final Clock clock;

    public AdminPaymentService(PaymentRepository payments, SessionUsageRepository usages, RiderRepository riders,
                               RiderBalanceService balances, PaymentEntryAssembler assembler, Clock clock) {
        this.payments = payments;
        this.usages = usages;
        this.riders = riders;
        this.balances = balances;
        this.assembler = assembler;
        this.clock = clock;
    }

    // W3's payment history for one rider: today's balance and every package, newest first, voided ones included.
    // No paging: a rider has a few payments a year.
    @Transactional(readOnly = true)
    public RiderPayments history(UUID riderId) {
        Rider rider = riders.findById(riderId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                ErrorCodes.RIDER_NOT_FOUND, "This rider does not exist."));
        LocalDate today = balances.today();
        List<Payment> history = payments.findByRiderIdOrderByRecordedAtDesc(riderId);
        return new RiderPayments(rider.getId(), rider.getName(), BalanceDto.of(balances.balanceOn(riderId, today)),
                assembler.entries(history, today));
    }

    @Transactional
    public PaymentResult record(UUID riderId, RecordPaymentRequest req, UUID adminId) {
        // The request is checked before the rider is read. The range is in the school time zone.
        LocalDate today = balances.today();
        YearMonth thisMonth = YearMonth.from(today);
        YearMonth start = req.startMonth();
        if (start.isBefore(thisMonth.minusMonths(1)) || start.isAfter(thisMonth.plusMonths(12))) {
            throw invalidField("startMonth", ErrorCodes.START_MONTH_OUT_OF_RANGE);
        }
        PaymentMethod method = PaymentMethod.fromCode(req.method())
                .orElseThrow(() -> invalidField("method", ErrorCodes.INVALID_METHOD));

        Rider rider = riders.findById(riderId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                ErrorCodes.RIDER_NOT_FOUND, "This rider does not exist."));
        int sessions = req.sessions() != null ? req.sessions() : Payment.defaultSessions(req.months());

        Payment p = payments.save(Payment.record(rider, req.amountEgp(), method, start, req.months(), sessions,
                adminId, clock.instant()));
        return new PaymentResult(assembler.entry(p, today), BalanceDto.of(balances.balanceOn(riderId, today)));
    }

    // W3's Void: a wrong payment is never edited or deleted. It stays in the history, marked voided with a reason,
    // and stops counting. Refused once sessions were used from it, so usage never silently moves to another package.
    // Upcoming, active and expired packages can all be voided. No notice goes to the trainer.
    @Transactional
    public PaymentResult voidPayment(UUID paymentId, VoidPaymentRequest req, UUID adminId) {
        // The lock serialises two voids, and a void with a session deduction, on the same payment.
        Payment p = payments.findByIdForUpdate(paymentId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                ErrorCodes.PAYMENT_NOT_FOUND, "This payment does not exist."));
        if (p.isVoided()) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.PAYMENT_ALREADY_VOIDED,
                    "This payment is already voided.");
        }
        long used = usages.countByPaymentId(paymentId);
        if (used > 0) {
            String sessionsWere = used == 1 ? "1 session was" : used + " sessions were";
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.PAYMENT_HAS_USAGE,
                    sessionsWere + " already used from this payment.", Map.of("details", Map.of("used", used)));
        }

        p.voidPayment(adminId, req.reason(), clock.instant());
        LocalDate today = balances.today();
        return new PaymentResult(assembler.entry(p, today),
                BalanceDto.of(balances.balanceOn(p.getRider().getId(), today)));
    }

    // Answered in the same 400 shape as a failed @Valid.
    private static ApiException invalidField(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Some fields are not valid.",
                Map.of("fieldErrors", List.of(new ApiFieldError(field, message))));
    }
}
