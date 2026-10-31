package com.equestrian.equis.service.payments;

import com.equestrian.equis.entity.payments.Payment;
import com.equestrian.equis.entity.payments.SessionUsage;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.repository.payments.PaymentRepository;
import com.equestrian.equis.repository.payments.SessionUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// Takes one session from each rider of a finished session, out of the oldest package valid on the session's date.
// All or nothing: if one rider has no session left, nothing is written for anyone.
// Runs inside the caller's transaction, so the finish and the usage rows commit together.
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class SessionUsageService {
    private final PaymentRepository payments;
    private final SessionUsageRepository usages;
    private final RiderBalanceService balances;

    public SessionUsageService(PaymentRepository payments, SessionUsageRepository usages,
                               RiderBalanceService balances) {
        this.payments = payments;
        this.usages = usages;
        this.balances = balances;
    }

    public List<SessionUsage> useOneSessionEach(Session session, Collection<Rider> riders, Instant now) {
        if (riders.isEmpty()) {
            return List.of();
        }
        // The session date decides which packages count, not the day the finish is pressed.
        LocalDate date = session.getSessionDate();
        List<UUID> riderIds = riders.stream().map(Rider::getId).toList();

        // Locked first, so a void on one of these packages can't overlap with the deduction.
        Map<UUID, Payment> locked = payments.lockValidOn(riderIds, date).stream()
                .collect(Collectors.toMap(Payment::getId, Function.identity()));
        Map<UUID, RiderBalance> balanceByRider = balances.balancesOn(riderIds, date);

        List<Payment> chosen = new ArrayList<>();
        List<Rider> missing = new ArrayList<>();
        for (Rider rider : riders) {
            balanceByRider.get(rider.getId()).packages().stream()
                    // A package recorded after the lock was taken is not used this time.
                    .filter(p -> p.left() > 0 && locked.containsKey(p.paymentId()))
                    .map(p -> locked.get(p.paymentId()))
                    .findFirst()
                    .ifPresentOrElse(chosen::add, () -> missing.add(rider));
        }
        if (!missing.isEmpty()) {
            throw new NoSessionsLeftException(missing);
        }
        return usages.saveAll(chosen.stream().map(p -> SessionUsage.of(p, session, now)).toList());
    }
}
