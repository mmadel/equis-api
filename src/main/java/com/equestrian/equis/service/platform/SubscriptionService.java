package com.equestrian.equis.service.platform;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.tenant.TenantTransactions;
import com.equestrian.equis.dto.platform.AddSubscriptionRequest;
import com.equestrian.equis.dto.platform.ClubAccessView;
import com.equestrian.equis.dto.platform.PlanView;
import com.equestrian.equis.dto.platform.StartMode;
import com.equestrian.equis.dto.platform.SubscriptionResult;
import com.equestrian.equis.dto.platform.SubscriptionStatus;
import com.equestrian.equis.dto.platform.SubscriptionView;
import com.equestrian.equis.entity.platform.BillingPeriod;
import com.equestrian.equis.entity.platform.Plan;
import com.equestrian.equis.entity.platform.Subscription;
import com.equestrian.equis.repository.club.ClubRepository;
import com.equestrian.equis.repository.platform.PlanRepository;
import com.equestrian.equis.repository.platform.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

// P3, a club's subscriptions: the plans to pick from, the history, adding a paid period and voiding a wrong one.
// Platform tables only, so everything runs in root mode, one transaction per call. Add and void lock the club row,
// so two clicks can't work out the same start.
@Service
public class SubscriptionService {
    private final ClubRepository clubs;
    private final PlanRepository plans;
    private final SubscriptionRepository subscriptions;
    private final ClubAccessService access;
    private final TenantTransactions tenants;
    private final ZoneId schoolZone;
    private final Clock clock;

    public SubscriptionService(ClubRepository clubs, PlanRepository plans, SubscriptionRepository subscriptions,
                               ClubAccessService access, TenantTransactions tenants,
                               @Value("${app.time-zone}") ZoneId schoolZone, Clock clock) {
        this.clubs = clubs;
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.access = access;
        this.tenants = tenants;
        this.schoolZone = schoolZone;
        this.clock = clock;
    }

    public List<PlanView> plans() {
        return tenants.asRoot(() -> plans.findAllByOrderByCodeAsc().stream()
                .map(p -> new PlanView(p.getCode(), p.getName(), p.getMaxTrainers(), p.getMaxRiders(),
                        p.getTrialDays(), p.getOnExpiry()))
                .toList());
    }

    // Newest start first, voided rows included.
    public List<SubscriptionView> history(UUID clubId) {
        return tenants.asRoot(() -> {
            if (!clubs.existsById(clubId)) {
                throw ClubErrors.notFound();
            }
            Instant now = clock.instant();
            Optional<Subscription> current = subscriptions.findCurrent(clubId, now);
            return subscriptions.findAllByClubIdOrderByStartsAtDesc(clubId).stream()
                    .map(s -> view(s, current, now))
                    .toList();
        });
    }

    // AFTER_CURRENT starts when the club's last non-voided row ends, or now if that is already past, so renewals
    // queue up; NOW starts now and becomes current because it is the latest start. The end is one calendar month or
    // year later on the school's calendar (31 Jan + 1 month = 28 Feb).
    public SubscriptionResult add(UUID clubId, AddSubscriptionRequest request, UUID superAdminId) {
        return tenants.asRoot(() -> {
            clubs.findByIdForUpdate(clubId).orElseThrow(ClubErrors::notFound);
            Plan plan = plans.findByCode(request.planCode().trim().toUpperCase(Locale.ROOT))
                    .orElseThrow(SubscriptionService::planNotFound);
            if (plan.isTrial()) {
                throw invalidBillingPeriod("The trial plan can't be added. Pick a paid plan.");
            }
            if (request.billingPeriod() == BillingPeriod.TRIAL) {
                throw invalidBillingPeriod("Pick monthly or annual.");
            }
            Instant now = clock.instant();
            Instant start;
            if (request.startMode() == StartMode.NOW) {
                if (subscriptions.existsByClubIdAndVoidedAtIsNullAndStartsAtAfter(clubId, now)) {
                    throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.UPCOMING_SUBSCRIPTION_EXISTS,
                            "This club has an upcoming subscription. Void it before starting a new one now.");
                }
                start = now;
            } else {
                start = subscriptions.findLatestEnd(clubId).filter(end -> end.isAfter(now)).orElse(now);
            }
            Subscription subscription;
            try {
                subscription = Subscription.create(clubId, plan, request.billingPeriod(), start,
                        endOf(start, request.billingPeriod()), request.note(), superAdminId);
            } catch (IllegalArgumentException e) {
                // Safety net: the checks above should already have refused it.
                throw invalidBillingPeriod(e.getMessage());
            }
            subscriptions.saveAndFlush(subscription);
            return result(subscription);
        });
    }

    // A voided row stays in the history and stops counting. Voiding an upgrade makes the row it replaced current
    // again, with its own end; a voided renewal never starts.
    public SubscriptionResult voidSubscription(UUID subscriptionId, String reason, UUID superAdminId) {
        return tenants.asRoot(() -> {
            UUID clubId = subscriptions.findClubIdById(subscriptionId)
                    .orElseThrow(SubscriptionService::subscriptionNotFound);
            String trimmed = reason == null ? "" : reason.trim();
            if (trimmed.length() < Subscription.MIN_VOID_REASON) {
                throw ClubErrors.invalidField("reason",
                        "Give a reason of at least " + Subscription.MIN_VOID_REASON + " characters.");
            }
            clubs.findByIdForUpdate(clubId).orElseThrow(ClubErrors::notFound);
            // Read after the lock, so a void that committed meanwhile is seen.
            Subscription subscription = subscriptions.findById(subscriptionId)
                    .orElseThrow(SubscriptionService::subscriptionNotFound);
            if (subscription.isVoided()) {
                throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.SUBSCRIPTION_ALREADY_VOIDED,
                        "This subscription is already voided.");
            }
            if (subscription.getBillingPeriod() == BillingPeriod.TRIAL) {
                throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.TRIAL_CANNOT_BE_VOIDED,
                        "The trial can't be voided. Suspend the club instead.");
            }
            subscription.voidWith(trimmed, superAdminId, clock.instant());
            subscriptions.flush();
            return result(subscription);
        });
    }

    private SubscriptionResult result(Subscription subscription) {
        Instant now = clock.instant();
        ClubAccess a = access.forClub(subscription.getClubId());
        ClubAccessView accessView = new ClubAccessView(a.state(), a.reason(),
                a.plan() == null ? null : a.plan().getCode(), a.period(), a.endsAt(), a.daysLeft());
        return new SubscriptionResult(
                view(subscription, subscriptions.findCurrent(subscription.getClubId(), now), now), accessView);
    }

    private Instant endOf(Instant start, BillingPeriod period) {
        ZonedDateTime from = start.atZone(schoolZone);
        return (period == BillingPeriod.ANNUAL ? from.plusYears(1) : from.plusMonths(1)).toInstant();
    }

    // current is the club's current subscription (findCurrent). A current row that has ended shows as ENDED.
    private static SubscriptionView view(Subscription s, Optional<Subscription> current, Instant now) {
        return new SubscriptionView(s.getId(), s.getPlan().getCode(), s.getPlan().getName(), s.getBillingPeriod(),
                s.getStartsAt(), s.getEndsAt(), s.getNote(), s.getCreatedAt(), statusOf(s, current, now),
                s.getVoidedAt(), s.getVoidReason());
    }

    private static SubscriptionStatus statusOf(Subscription s, Optional<Subscription> current, Instant now) {
        if (s.isVoided()) {
            return SubscriptionStatus.VOIDED;
        }
        if (s.getStartsAt().isAfter(now)) {
            return SubscriptionStatus.UPCOMING;
        }
        if (!s.getEndsAt().isAfter(now)) {
            return SubscriptionStatus.ENDED;
        }
        boolean isCurrent = current.map(c -> c.getId().equals(s.getId())).orElse(false);
        return isCurrent ? SubscriptionStatus.CURRENT : SubscriptionStatus.REPLACED;
    }

    private static ApiException planNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.PLAN_NOT_FOUND, "This plan doesn't exist.");
    }

    private static ApiException subscriptionNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.SUBSCRIPTION_NOT_FOUND,
                "This subscription doesn't exist.");
    }

    private static ApiException invalidBillingPeriod(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_BILLING_PERIOD, message);
    }
}
