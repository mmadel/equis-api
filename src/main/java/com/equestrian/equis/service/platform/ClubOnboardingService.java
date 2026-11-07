package com.equestrian.equis.service.platform;

import com.equestrian.equis.common.error.Constraints;
import com.equestrian.equis.common.tenant.TenantTransactions;
import com.equestrian.equis.dto.platform.ClubDetail;
import com.equestrian.equis.dto.platform.CreateClubRequest;
import com.equestrian.equis.entity.accounts.AdminAccount;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.SubjectType;
import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.entity.platform.BillingPeriod;
import com.equestrian.equis.entity.platform.Plan;
import com.equestrian.equis.entity.platform.Subscription;
import com.equestrian.equis.repository.accounts.AdminAccountRepository;
import com.equestrian.equis.repository.club.ClubRepository;
import com.equestrian.equis.repository.platform.PlanRepository;
import com.equestrian.equis.repository.platform.SubscriptionRepository;
import com.equestrian.equis.service.accounts.AccountLinkCreatedEvent;
import com.equestrian.equis.service.accounts.AccountLinkService;
import com.equestrian.equis.service.accounts.AccountLinkService.IssuedLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

// P2: the super admin adds a riding school. In one request the server creates the club, starts its trial, invites
// its first admin and emails them a set-password link with the club code.
//
// Two transactions, because one transaction works for one club only (TenantTransactions):
//   1. root mode: the club and its trial subscription (global tables)
//   2. in the new club: the admin account and its link; the email goes after this one commits
// If the second fails, the first is undone in a third (root) transaction, so no club is left without an admin.
// Cancel reasons, payment methods and levels are global reference data, so a new club has nothing to seed.
@Service
public class ClubOnboardingService {
    private static final Logger log = LoggerFactory.getLogger(ClubOnboardingService.class);

    // The club table's rule (ck_club_code): lowercase letters, digits and inner hyphens, 3–30 characters.
    private static final Pattern CODE = Pattern.compile("^[a-z0-9][a-z0-9-]{1,28}[a-z0-9]$");
    // Words that may become paths or host names of the platform.
    private static final Set<String> RESERVED_CODES = Set.of("platform", "admin", "api", "www");

    private final ClubRepository clubs;
    private final PlanRepository plans;
    private final SubscriptionRepository subscriptions;
    private final AdminAccountRepository admins;
    private final AccountLinkService links;
    private final ApplicationEventPublisher events;
    private final TenantTransactions tenants;
    private final ClubAdminService clubAdmin;
    private final ZoneId schoolZone;
    private final Clock clock;

    public ClubOnboardingService(ClubRepository clubs, PlanRepository plans, SubscriptionRepository subscriptions,
                                 AdminAccountRepository admins, AccountLinkService links,
                                 ApplicationEventPublisher events, TenantTransactions tenants,
                                 ClubAdminService clubAdmin, @Value("${app.time-zone}") ZoneId schoolZone,
                                 Clock clock) {
        this.clubs = clubs;
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.admins = admins;
        this.links = links;
        this.events = events;
        this.tenants = tenants;
        this.clubAdmin = clubAdmin;
        this.schoolZone = schoolZone;
        this.clock = clock;
    }

    // Call outside a transaction: each step opens its own.
    public ClubDetail create(CreateClubRequest request, UUID superAdminId) {
        String code = normalizeCode(request.code());
        String name = request.name().trim();
        String adminName = request.admin().name().trim();
        String adminEmail = request.admin().email().trim().toLowerCase(Locale.ROOT);

        Club club = tenants.asRoot(() -> createClubWithTrial(code, name, superAdminId));
        try {
            tenants.inClub(club.getId(), () -> {
                inviteAdmin(club, adminName, adminEmail);
                return null;
            });
        } catch (RuntimeException e) {
            undo(club, e);
            throw e;
        }
        return clubAdmin.get(club.getId());
    }

    private Club createClubWithTrial(String code, String name, UUID superAdminId) {
        if (clubs.existsByCode(code)) {
            throw ClubErrors.codeTaken();
        }
        Plan trial = plans.findByTrialDaysIsNotNull().orElseThrow(ClubErrors::noPlans);
        Club club = new Club();
        club.setCode(code);
        club.setName(name);
        club.setCreatedBy(superAdminId);
        try {
            clubs.saveAndFlush(club);
        } catch (DataIntegrityViolationException e) {
            // Two clubs with the same code at the same moment.
            if (Constraints.nameOf(e).filter("uq_club_code"::equals).isPresent()) {
                throw ClubErrors.codeTaken();
            }
            throw e;
        }
        // The trial ends at the same clock time, trial_days later on the school's calendar.
        Instant start = clock.instant();
        Instant end = start.atZone(schoolZone).plusDays(trial.getTrialDays()).toInstant();
        subscriptions.save(Subscription.create(club.getId(), trial, BillingPeriod.TRIAL, start, end, null,
                superAdminId));
        return club;
    }

    private void inviteAdmin(Club club, String name, String email) {
        AdminAccount admin = admins.saveAndFlush(AdminAccount.invited(name, email));
        IssuedLink link = links.issue(admin, InvitePurpose.INVITE);
        events.publishEvent(new AccountLinkCreatedEvent(SubjectType.ADMIN, admin.getId(), admin.getName(),
                admin.getEmail(), InvitePurpose.INVITE, link.rawToken(), link.expiresAt(),
                club.getName(), club.getCode()));
    }

    // Removes the club and its trial. If that fails too, the first error is still the one thrown.
    private void undo(Club club, RuntimeException cause) {
        try {
            tenants.asRoot(() -> {
                subscriptions.deleteByClubId(club.getId());
                clubs.deleteById(club.getId());
                return null;
            });
        } catch (RuntimeException e) {
            log.error("Could not remove club {} after its admin could not be created", club.getCode(), e);
            cause.addSuppressed(e);
        }
    }

    // Trimmed and lowercased, so "Cairo-EQ " and "cairo-eq" are the same code.
    private static String normalizeCode(String raw) {
        String code = raw.trim().toLowerCase(Locale.ROOT);
        if (!CODE.matcher(code).matches()) {
            throw ClubErrors.invalidField("code",
                    "Use 3 to 30 lowercase letters, digits or hyphens, starting and ending with a letter or digit.");
        }
        if (RESERVED_CODES.contains(code)) {
            throw ClubErrors.invalidField("code", "This code is reserved. Choose another one.");
        }
        return code;
    }
}
