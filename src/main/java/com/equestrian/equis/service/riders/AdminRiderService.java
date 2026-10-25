package com.equestrian.equis.service.riders;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.riders.AdminRiderList;
import com.equestrian.equis.dto.riders.AdminRiderRow;
import com.equestrian.equis.dto.riders.RiderTotals;
import com.equestrian.equis.dto.riders.TrainerCard;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.assessment.BookingStatus;
import com.equestrian.equis.entity.levels.Levels;
import com.equestrian.equis.entity.payments.RiderPaymentStatus;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// W3: every rider with their trainer and assessor, plus the totals and trainer cards. One query loads all riders
// (a school has a few hundred at most); the filters are applied here and never change the totals or cards.
@Service
public class AdminRiderService {
    private static final String UNASSIGNED = HowAssigned.UNASSIGNED.value();
    // Riders needing a trainer first, then by level, then by name.
    private static final Comparator<AdminRiderRow> LIST_ORDER = Comparator
            .<AdminRiderRow, Boolean>comparing(r -> !r.howAssigned().equals(UNASSIGNED))
            .thenComparingInt(AdminRiderRow::levelId)
            .thenComparing(AdminRiderRow::name, String.CASE_INSENSITIVE_ORDER);
    // Active trainers first, then by name.
    private static final Comparator<Trainer> CARD_ORDER = Comparator
            .<Trainer, Boolean>comparing(t -> t.getStatus() != TrainerStatus.ACTIVE)
            .thenComparing(Trainer::getName, String.CASE_INSENSITIVE_ORDER);

    private final RiderRepository riders;
    private final TrainerRepository trainers;
    private final AdminRiderRowAssembler rows;

    public AdminRiderService(RiderRepository riders, TrainerRepository trainers, AdminRiderRowAssembler rows) {
        this.riders = riders;
        this.trainers = trainers;
        this.rows = rows;
    }

    // All filters optional. trainer: the rider's current trainer; an unknown id gives no rows.
    // unassigned: true keeps only riders needing a trainer. q: part of the name, or digits of the phone.
    // payment: "active" or "awaiting_payment", from today's balance.
    @Transactional(readOnly = true)
    public AdminRiderList list(Integer level, UUID trainer, Boolean unassigned, String q, String payment) {
        if (level != null && !Levels.isValid(level)) {
            throw invalidField("level", ErrorCodes.INVALID_LEVEL);
        }
        RiderPaymentStatus paymentStatus = payment == null ? null : Arrays.stream(RiderPaymentStatus.values())
                .filter(s -> s.value().equals(payment))
                .findFirst()
                .orElseThrow(() -> invalidField("payment", ErrorCodes.INVALID_PAYMENT_STATUS));
        List<AdminRiderRow> all = rows.rows(riders.findAdminView(BookingStatus.PLACED));

        Map<String, Long> byHow = all.stream()
                .collect(Collectors.groupingBy(AdminRiderRow::howAssigned, Collectors.counting()));
        RiderTotals totals = new RiderTotals(all.size(),
                byHow.getOrDefault(HowAssigned.FROM_ASSESSMENT.value(), 0L),
                byHow.getOrDefault(HowAssigned.CHANGED_MANUALLY.value(), 0L),
                byHow.getOrDefault(HowAssigned.SET_MANUALLY.value(), 0L),
                all.stream().filter(AdminRiderService::needsTrainer).count(),
                all.stream().filter(r -> r.balance().status() == RiderPaymentStatus.AWAITING_PAYMENT).count());

        Map<UUID, Long> perTrainer = all.stream()
                .filter(r -> r.trainer() != null)
                .collect(Collectors.groupingBy(r -> r.trainer().id(), Collectors.counting()));
        List<TrainerCard> cards = trainers.findByStatusIn(List.of(TrainerStatus.ACTIVE, TrainerStatus.DISABLED))
                .stream()
                .sorted(CARD_ORDER)
                .map(t -> new TrainerCard(t.getId(), t.getName(), t.getStatus().name().toLowerCase(Locale.ROOT),
                        perTrainer.getOrDefault(t.getId(), 0L)))
                .toList();

        List<AdminRiderRow> shown = all.stream()
                .filter(r -> level == null || r.levelId() == level)
                .filter(r -> trainer == null || (r.trainer() != null && r.trainer().id().equals(trainer)))
                .filter(r -> !Boolean.TRUE.equals(unassigned) || needsTrainer(r))
                .filter(r -> paymentStatus == null || r.balance().status() == paymentStatus)
                .filter(matches(q))
                .sorted(LIST_ORDER)
                .toList();
        return new AdminRiderList(totals, cards, shown);
    }

    // W3's "needs a trainer" total (totals.unassigned), for the admin Overview. One query: no balances needed.
    @Transactional(readOnly = true)
    public long countNeedingTrainer() {
        return riders.findAdminView(BookingStatus.PLACED).stream()
                .filter(v -> rows.howAssigned(v).equals(UNASSIGNED))
                .count();
    }

    // Answered in the same 400 shape as a failed @Valid.
    private static ApiException invalidField(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Some fields are not valid.",
                Map.of("fieldErrors", List.of(new ApiFieldError(field, message))));
    }

    private static boolean needsTrainer(AdminRiderRow r) {
        return r.howAssigned().equals(UNASSIGNED);
    }

    // The name contains q, ignoring case; or, when q has digits, the phone contains those digits (as W2).
    private static Predicate<AdminRiderRow> matches(String q) {
        if (q == null || q.isBlank()) {
            return r -> true;
        }
        String text = q.trim().toLowerCase(Locale.ROOT);
        String digits = q.replaceAll("\\D", "");
        return r -> r.name().toLowerCase(Locale.ROOT).contains(text)
                || (!digits.isEmpty() && r.phone() != null && r.phone().contains(digits));
    }
}
