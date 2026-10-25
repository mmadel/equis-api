package com.equestrian.equis.service.payments;

import com.equestrian.equis.dto.payments.PaymentEntry;
import com.equestrian.equis.entity.payments.Payment;
import com.equestrian.equis.repository.payments.SessionUsageRepository;
import com.equestrian.equis.repository.payments.UsedCount;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// Turns packages into PaymentEntry rows for a day. One usage query, however many packages.
@Component
public class PaymentEntryAssembler {
    private final SessionUsageRepository sessionUsageRepository;

    public PaymentEntryAssembler(SessionUsageRepository sessionUsageRepository) {
        this.sessionUsageRepository = sessionUsageRepository;
    }

    public List<PaymentEntry> entries(List<Payment> payments, LocalDate today) {
        if (payments.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = payments.stream().map(Payment::getId).toList();
        Map<UUID, Long> used = sessionUsageRepository.countUsedByPayment(ids).stream()
                .collect(Collectors.toMap(UsedCount::getPaymentId, UsedCount::getUsed));
        return payments.stream()
                .map(p -> entry(p, Math.toIntExact(used.getOrDefault(p.getId(), 0L)), today))
                .toList();
    }

    public PaymentEntry entry(Payment p, LocalDate today) {
        return entries(List.of(p), today).getFirst();
    }

    private static PaymentEntry entry(Payment p, int used, LocalDate today) {
        PaymentState state = PaymentState.of(p, used, today);
        return new PaymentEntry(p.getId(), p.getRider().getId(), p.getAmountEgp(), p.getMethod(),
                YearMonth.from(p.getStartMonth()), p.getMonths(), p.getSessions(), p.getValidUntil(),
                used, state.left(p, used), state.value(), p.getRecordedAt(), p.getVoidedAt(), p.getVoidReason());
    }
}
