package com.equestrian.equis.service.platform;

import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.entity.club.ClubStatus;
import com.equestrian.equis.entity.platform.OnExpiry;
import com.equestrian.equis.entity.platform.Subscription;
import com.equestrian.equis.repository.club.ClubRepository;
import com.equestrian.equis.repository.platform.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

// Works out what a club may do now. Computed on every call, never stored. Enforcing it on requests is a later ticket.
//   club suspended                          -> BLOCKED   / SUSPENDED
//   no current subscription                 -> BLOCKED   / NO_SUBSCRIPTION
//   current ended, plan on_expiry BLOCK     -> BLOCKED   / TRIAL_ENDED
//   current ended, plan on_expiry READ_ONLY -> READ_ONLY / SUBSCRIPTION_ENDED
//   otherwise                               -> ACTIVE
@Service
public class ClubAccessService {
    private final ClubRepository clubs;
    private final SubscriptionRepository subscriptions;
    private final ZoneId schoolZone;
    private final Clock clock;

    public ClubAccessService(ClubRepository clubs, SubscriptionRepository subscriptions,
                             @Value("${app.time-zone}") ZoneId schoolZone, Clock clock) {
        this.clubs = clubs;
        this.subscriptions = subscriptions;
        this.schoolZone = schoolZone;
        this.clock = clock;
    }

    // current is the club's current subscription (SubscriptionRepository.findCurrent), which may have ended.
    // Its plan must be loadable: call inside a transaction, or with the plan already fetched.
    public ClubAccess stateOf(Club club, Optional<Subscription> current, Instant now) {
        boolean suspended = club.getStatus() == ClubStatus.SUSPENDED;
        Subscription sub = current.orElse(null);
        if (sub == null) {
            return new ClubAccess(AccessState.BLOCKED,
                    suspended ? AccessReason.SUSPENDED : AccessReason.NO_SUBSCRIPTION, null, null, null, 0);
        }
        AccessState state;
        AccessReason reason;
        if (suspended) {
            state = AccessState.BLOCKED;
            reason = AccessReason.SUSPENDED;
        } else if (!sub.isValidAt(now)) {
            boolean block = sub.getPlan().getOnExpiry() == OnExpiry.BLOCK;
            state = block ? AccessState.BLOCKED : AccessState.READ_ONLY;
            reason = block ? AccessReason.TRIAL_ENDED : AccessReason.SUBSCRIPTION_ENDED;
        } else {
            state = AccessState.ACTIVE;
            reason = null;
        }
        return new ClubAccess(state, reason, sub.getPlan(), sub.getBillingPeriod(), sub.getEndsAt(),
                daysLeft(now, sub.getEndsAt()));
    }

    @Transactional(readOnly = true)
    public ClubAccess forClub(UUID clubId) {
        Instant now = clock.instant();
        Club club = clubs.findById(clubId).orElseThrow(ClubErrors::notFound);
        return stateOf(club, subscriptions.findCurrent(clubId, now), now);
    }

    // Whole days from now until the end on the school's calendar (a day stays a day across a clock change),
    // rounded down; 0 once less than a day is left or it has ended.
    public int daysLeft(Instant now, Instant endsAt) {
        long days = ChronoUnit.DAYS.between(now.atZone(schoolZone), endsAt.atZone(schoolZone));
        return (int) Math.max(0, days);
    }
}
