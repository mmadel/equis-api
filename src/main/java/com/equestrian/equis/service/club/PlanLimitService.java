package com.equestrian.equis.service.club;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.dto.club.ClubUsageView;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.platform.Plan;
import com.equestrian.equis.entity.platform.Subscription;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.assessment.MemberRepository;
import com.equestrian.equis.repository.club.ClubRepository;
import com.equestrian.equis.repository.platform.SubscriptionRepository;
import com.equestrian.equis.service.platform.ClubAccess;
import com.equestrian.equis.service.platform.ClubAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

// The plan's trainer and rider limits, checked on the admin's actions that add one: intake and reopen of a person,
// invite and enable of a trainer. Never on what a trainer does. Counted like the platform usage (ClubUsageService):
//   trainers: not disabled (invited and active)
//   riders:   people (member) not closed, so applicants count too
// The plan is read from the database on every check, so a plan changed by ops/upsert-plan.sql applies at once.
// A downgrade removes nothing: the club just can't add more until it is back under the limit.
@Service
public class PlanLimitService {
    private final ClubRepository clubs;
    private final SubscriptionRepository subscriptions;
    private final TrainerRepository trainers;
    private final MemberRepository members;
    private final ClubAccessService access;
    private final Clock clock;

    public PlanLimitService(ClubRepository clubs, SubscriptionRepository subscriptions, TrainerRepository trainers,
                            MemberRepository members, ClubAccessService access, Clock clock) {
        this.clubs = clubs;
        this.subscriptions = subscriptions;
        this.trainers = trainers;
        this.members = members;
        this.access = access;
        this.clock = clock;
    }

    // Call inside the transaction that adds the trainer: the club row stays locked until it ends.
    @Transactional(propagation = Propagation.MANDATORY)
    public void checkCanAddTrainer() {
        check(LimitedResource.TRAINERS);
    }

    // Call inside the transaction that adds or reopens the person: the club row stays locked until it ends.
    @Transactional(propagation = Propagation.MANDATORY)
    public void checkCanAddPerson() {
        check(LimitedResource.RIDERS);
    }

    // GET /admin/club/usage. No lock: it only reads.
    @Transactional(readOnly = true)
    public ClubUsageView usage() {
        ClubAccess club = access.forClub(currentClub());
        Plan plan = club.plan();
        return new ClubUsageView(
                plan == null ? null : plan.getCode(),
                plan == null ? null : plan.getName(),
                club.period(), club.endsAt(), club.daysLeft(), club.state(),
                new ClubUsageView.Count(count(LimitedResource.TRAINERS), plan == null ? null : plan.getMaxTrainers()),
                new ClubUsageView.Count(count(LimitedResource.RIDERS), plan == null ? null : plan.getMaxRiders()));
    }

    private void check(LimitedResource resource) {
        UUID clubId = currentClub();
        // Two admins adding at the same moment: the second waits here until the first commits, then counts again.
        clubs.findByIdForUpdate(clubId)
                .orElseThrow(() -> new IllegalStateException("Club " + clubId + " doesn't exist"));
        Instant now = clock.instant();
        Optional<Plan> plan = subscriptions.findCurrent(clubId, now)
                .filter(s -> s.isValidAt(now))
                .map(Subscription::getPlan);
        if (plan.isEmpty()) {
            // ClubAccessFilter stops read-only and blocked clubs before this; refuse if one gets here anyway.
            throw new PlanLimitReachedException(resource, 0, count(resource));
        }
        OptionalInt limit = resource == LimitedResource.TRAINERS
                ? plan.get().trainerLimit()
                : plan.get().riderLimit();
        if (limit.isEmpty()) {
            return; // unlimited
        }
        long used = count(resource);
        if (used >= limit.getAsInt()) {
            throw new PlanLimitReachedException(resource, limit.getAsInt(), used);
        }
    }

    // Spring Data keeps the counts to the current club (@TenantId).
    private long count(LimitedResource resource) {
        return switch (resource) {
            case TRAINERS -> trainers.countByStatusNot(TrainerStatus.DISABLED);
            case RIDERS -> members.countByClosedAtIsNull();
        };
    }

    private static UUID currentClub() {
        UUID clubId = TenantContext.current();
        if (clubId == null) {
            throw new IllegalStateException("Plan limits are checked in a club");
        }
        return clubId;
    }
}
