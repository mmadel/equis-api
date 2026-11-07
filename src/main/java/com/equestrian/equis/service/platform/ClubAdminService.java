package com.equestrian.equis.service.platform;

import com.equestrian.equis.common.tenant.TenantTransactions;
import com.equestrian.equis.dto.platform.ChangeClubStatusRequest;
import com.equestrian.equis.dto.platform.ClubDetail;
import com.equestrian.equis.dto.platform.ClubPage;
import com.equestrian.equis.dto.platform.ClubRow;
import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.entity.club.ClubStatus;
import com.equestrian.equis.entity.platform.Plan;
import com.equestrian.equis.entity.platform.Subscription;
import com.equestrian.equis.repository.accounts.AdminAccountRepository;
import com.equestrian.equis.repository.club.ClubRepository;
import com.equestrian.equis.repository.platform.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// P1 and the club page: the super admin's list of clubs, one club, and suspend / reactivate.
// Everything runs in root mode, one transaction per call. The access state is worked out per club, so the list is
// filtered, sorted and paged in memory; there are few clubs. Usage is counted only for the page shown.
@Service
public class ClubAdminService {
    public static final int MIN_SUSPEND_REASON = 5;

    private final ClubRepository clubs;
    private final SubscriptionRepository subscriptions;
    private final AdminAccountRepository admins;
    private final ClubAccessService access;
    private final ClubUsageService usage;
    private final TenantTransactions tenants;
    private final ZoneId schoolZone;
    private final Clock clock;

    public ClubAdminService(ClubRepository clubs, SubscriptionRepository subscriptions, AdminAccountRepository admins,
                            ClubAccessService access, ClubUsageService usage, TenantTransactions tenants,
                            @Value("${app.time-zone}") ZoneId schoolZone, Clock clock) {
        this.clubs = clubs;
        this.subscriptions = subscriptions;
        this.admins = admins;
        this.access = access;
        this.usage = usage;
        this.tenants = tenants;
        this.schoolZone = schoolZone;
        this.clock = clock;
    }

    public ClubPage list(ClubFilter filter, ClubSort sort, boolean descending, int page, int size) {
        return tenants.asRoot(() -> {
            Instant now = clock.instant();
            List<Club> all = clubs.findAll();
            Map<UUID, Subscription> current = subscriptions
                    .findCurrentForClubs(all.stream().map(Club::getId).toList(), now).stream()
                    .collect(Collectors.toMap(Subscription::getClubId, Function.identity()));
            List<Entry> matching = all.stream()
                    .map(c -> new Entry(c, access.stateOf(c, Optional.ofNullable(current.get(c.getId())), now)))
                    .filter(e -> matches(filter, e, now))
                    .sorted(order(sort, descending))
                    .toList();
            int from = (int) Math.min((long) page * size, matching.size());
            int to = Math.min(from + size, matching.size());
            List<Entry> shown = matching.subList(from, to);
            Map<UUID, Usage> used = usage.usageFor(shown.stream().map(e -> e.club().getId()).toList(), plans(shown));
            List<ClubRow> rows = shown.stream().map(e -> row(e, used.get(e.club().getId()))).toList();
            return new ClubPage(rows, page, size, matching.size());
        });
    }

    public ClubDetail get(UUID id) {
        return tenants.asRoot(() -> detail(clubs.findById(id).orElseThrow(ClubErrors::notFound)));
    }

    // Suspending needs a reason and blocks the club; reactivating clears the reason. Asking for the status the club
    // already has is refused, so a double click can't overwrite the first suspension's reason and time.
    public ClubDetail changeStatus(UUID id, ChangeClubStatusRequest request) {
        return tenants.asRoot(() -> {
            Club club = clubs.findByIdForUpdate(id).orElseThrow(ClubErrors::notFound);
            if (request.status() == ClubStatus.SUSPENDED) {
                String reason = request.reason() == null ? "" : request.reason().trim();
                if (reason.length() < MIN_SUSPEND_REASON) {
                    throw ClubErrors.invalidField("reason",
                            "Give a reason of at least " + MIN_SUSPEND_REASON + " characters.");
                }
                if (club.getStatus() == ClubStatus.SUSPENDED) {
                    throw ClubErrors.invalidStatus("This club is already suspended.");
                }
                club.setStatus(ClubStatus.SUSPENDED);
                club.setSuspendedAt(clock.instant());
                club.setSuspendedReason(reason);
            } else {
                if (club.getStatus() == ClubStatus.ACTIVE) {
                    throw ClubErrors.invalidStatus("This club is already active.");
                }
                club.setStatus(ClubStatus.ACTIVE);
                club.setSuspendedAt(null);
                club.setSuspendedReason(null);
            }
            // Saved at commit; the answer is built from the changed entity.
            return detail(club);
        });
    }

    private ClubDetail detail(Club club) {
        Instant now = clock.instant();
        ClubAccess a = access.stateOf(club, subscriptions.findCurrent(club.getId(), now), now);
        Map<UUID, Plan> plan = new HashMap<>();
        if (a.plan() != null) {
            plan.put(club.getId(), a.plan());
        }
        Usage u = usage.usageFor(List.of(club.getId()), plan).get(club.getId());
        List<ClubDetail.Admin> clubAdmins = admins.findByClubIdOrderByCreatedAtAscEmailAsc(club.getId()).stream()
                .map(admin -> new ClubDetail.Admin(admin.getName(), admin.getEmail(), admin.getStatus()))
                .toList();
        return new ClubDetail(club.getId(), club.getCode(), club.getName(), club.getStatus(),
                a.state(), a.reason(), planCode(a), planName(a), a.period(), a.endsAt(), a.daysLeft(),
                u.trainers(), u.maxTrainers(), u.riders(), u.maxRiders(),
                club.getContactPhone(), club.getContactEmail(), club.getAddress(), club.getCreatedAt(),
                club.getSuspendedAt(), club.getSuspendedReason(), clubAdmins);
    }

    private static ClubRow row(Entry e, Usage u) {
        Club club = e.club();
        ClubAccess a = e.access();
        return new ClubRow(club.getId(), club.getCode(), club.getName(), club.getStatus(),
                a.state(), a.reason(), planCode(a), planName(a), a.period(), a.endsAt(), a.daysLeft(),
                u.trainers(), u.maxTrainers(), u.riders(), u.maxRiders());
    }

    private boolean matches(ClubFilter filter, Entry e, Instant now) {
        if (filter.q() != null && !filter.q().isBlank()) {
            String q = filter.q().trim().toLowerCase(Locale.ROOT);
            if (!e.club().getName().toLowerCase(Locale.ROOT).contains(q) && !e.club().getCode().contains(q)) {
                return false;
            }
        }
        if (filter.state() != null && e.access().state() != filter.state()) {
            return false;
        }
        if (filter.expiringWithinDays() != null) {
            Instant endsAt = e.access().endsAt();
            Instant limit = now.atZone(schoolZone).plusDays(filter.expiringWithinDays()).toInstant();
            return endsAt != null && endsAt.isAfter(now) && !endsAt.isAfter(limit);
        }
        return true;
    }

    // Clubs without a subscription sort after the others by end date, whichever the direction. Ties go by code.
    private static Comparator<Entry> order(ClubSort sort, boolean descending) {
        Comparator<Entry> order = switch (sort) {
            case NAME -> Comparator.comparing(e -> e.club().getName(), String.CASE_INSENSITIVE_ORDER);
            case CODE -> Comparator.comparing(e -> e.club().getCode());
            case ENDS_AT -> Comparator.comparing(e -> e.access().endsAt(),
                    Comparator.nullsLast(descending ? Comparator.<Instant>reverseOrder() : Comparator.naturalOrder()));
        };
        if (descending && sort != ClubSort.ENDS_AT) {
            order = order.reversed();
        }
        return order.thenComparing(e -> e.club().getCode());
    }

    private static Map<UUID, Plan> plans(List<Entry> entries) {
        Map<UUID, Plan> plans = new HashMap<>();
        entries.stream().filter(e -> e.access().plan() != null)
                .forEach(e -> plans.put(e.club().getId(), e.access().plan()));
        return plans;
    }

    private static String planCode(ClubAccess a) {
        return a.plan() == null ? null : a.plan().getCode();
    }

    private static String planName(ClubAccess a) {
        return a.plan() == null ? null : a.plan().getName();
    }

    private record Entry(Club club, ClubAccess access) {
    }
}
