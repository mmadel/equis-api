package com.equestrian.equis.service.payments;

import com.equestrian.equis.dto.payments.PaymentEntry;
import com.equestrian.equis.dto.payments.PaymentUsage;
import com.equestrian.equis.entity.payments.Payment;
import com.equestrian.equis.repository.payments.SessionUsageRepository;
import com.equestrian.equis.repository.payments.UsageLine;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// Turns packages into PaymentEntry rows for a day, each with the sessions used from it. One usage query, however
// many packages; used is counted from the same lines, so it always matches them.
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
        Map<UUID, List<PaymentUsage>> usages = sessionUsageRepository.findUsageLines(ids).stream()
                .collect(Collectors.groupingBy(UsageLine::paymentId,
                        Collectors.mapping(UsageLine::toDto, Collectors.toList())));
        return payments.stream()
                .map(p -> entry(p, usages.getOrDefault(p.getId(), List.of()), today))
                .toList();
    }

    public PaymentEntry entry(Payment p, LocalDate today) {
        return entries(List.of(p), today).getFirst();
    }

    private static PaymentEntry entry(Payment p, List<PaymentUsage> usages, LocalDate today) {
        int used = usages.size();
        PaymentState state = PaymentState.of(p, used, today);
        return new PaymentEntry(p.getId(), p.getRider().getId(), p.getAmountEgp(), p.getMethod(),
                YearMonth.from(p.getStartMonth()), p.getMonths(), p.getSessions(), p.getValidUntil(),
                used, state.left(p, used), state.value(), p.getRecordedAt(), p.getVoidedAt(), p.getVoidReason(),
                List.copyOf(usages));
    }
}
