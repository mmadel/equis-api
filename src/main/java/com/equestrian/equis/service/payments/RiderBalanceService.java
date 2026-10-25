package com.equestrian.equis.service.payments;

import com.equestrian.equis.entity.payments.Payment;
import com.equestrian.equis.entity.payments.RiderPaymentStatus;
import com.equestrian.equis.repository.payments.PaymentRepository;
import com.equestrian.equis.repository.payments.SessionUsageRepository;
import com.equestrian.equis.repository.payments.UsedCount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// How many sessions a rider has left on a date, and which package the next one comes from.
// Sessions come out of the package that expires first; ties go to the earlier start month, then the one recorded first.
@Service
@Transactional(readOnly = true)
public class RiderBalanceService {
    static final Comparator<Payment> OLDEST_FIRST = Comparator
            .comparing(Payment::getValidUntil)
            .thenComparing(Payment::getStartMonth)
            .thenComparing(Payment::getRecordedAt);

    private final PaymentRepository paymentRepository;
    private final SessionUsageRepository sessionUsageRepository;
    private final Clock clock;
    private final ZoneId schoolZone;

    public RiderBalanceService(PaymentRepository paymentRepository, SessionUsageRepository sessionUsageRepository,
                               Clock clock, @Value("${app.time-zone}") ZoneId schoolZone) {
        this.paymentRepository = paymentRepository;
        this.sessionUsageRepository = sessionUsageRepository;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    public LocalDate today() {
        return LocalDate.now(clock.withZone(schoolZone));
    }

    // Every rider asked for gets an entry, in the order given. Two queries, however many riders.
    public Map<UUID, RiderBalance> balancesOn(Collection<UUID> riderIds, LocalDate date) {
        Map<UUID, RiderBalance> result = new LinkedHashMap<>();
        if (riderIds.isEmpty()) {
            return result;
        }
        Set<UUID> ids = Set.copyOf(riderIds);
        List<Payment> valid = paymentRepository.findValidOn(ids, date);
        Map<UUID, Long> used = usedByPayment(valid);
        Map<UUID, List<Payment>> byRider = valid.stream()
                .collect(Collectors.groupingBy(p -> p.getRider().getId()));
        for (UUID riderId : riderIds) {
            List<Payment> packages = byRider.getOrDefault(riderId, List.of());
            result.put(riderId, balance(riderId, packages, used));
        }
        return result;
    }

    public RiderBalance balanceOn(UUID riderId, LocalDate date) {
        return balancesOn(List.of(riderId), date).get(riderId);
    }

    // The package the next session on sessionDate comes out of, or empty when the rider has none left that day.
    public Optional<UUID> packageToUse(UUID riderId, LocalDate sessionDate) {
        return balanceOn(riderId, sessionDate).packages().stream()
                .filter(p -> p.left() > 0)
                .map(PackageBalance::paymentId)
                .findFirst();
    }

    private Map<UUID, Long> usedByPayment(List<Payment> payments) {
        if (payments.isEmpty()) {
            return Map.of();
        }
        List<UUID> paymentIds = payments.stream().map(Payment::getId).toList();
        return sessionUsageRepository.countUsedByPayment(paymentIds).stream()
                .collect(Collectors.toMap(UsedCount::getPaymentId, UsedCount::getUsed));
    }

    private static RiderBalance balance(UUID riderId, List<Payment> payments, Map<UUID, Long> used) {
        if (payments.isEmpty()) {
            return RiderBalance.empty(riderId);
        }
        List<PackageBalance> packages = new ArrayList<>();
        int sessionsLeft = 0;
        LocalDate nextExpiry = null;
        for (Payment p : payments.stream().sorted(OLDEST_FIRST).toList()) {
            int usedCount = Math.toIntExact(used.getOrDefault(p.getId(), 0L));
            int left = Math.max(0, p.getSessions() - usedCount);
            packages.add(new PackageBalance(p.getId(), p.getStartMonth(), p.getValidUntil(),
                    p.getSessions(), usedCount, left));
            sessionsLeft += left;
            if (left > 0 && nextExpiry == null) {
                nextExpiry = p.getValidUntil();
            }
        }
        return new RiderBalance(riderId, sessionsLeft, RiderPaymentStatus.fromBalance(sessionsLeft), nextExpiry,
                List.copyOf(packages));
    }
}
