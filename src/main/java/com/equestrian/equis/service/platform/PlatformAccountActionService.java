package com.equestrian.equis.service.platform;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.tenant.TenantTransactions;
import com.equestrian.equis.dto.platform.AccountType;
import com.equestrian.equis.dto.platform.PlatformAccountReset;
import com.equestrian.equis.dto.platform.PlatformAccountStatus;
import com.equestrian.equis.entity.accounts.AdminAccount;
import com.equestrian.equis.entity.accounts.AdminStatus;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.SubjectType;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.entity.platform.AccountActionType;
import com.equestrian.equis.entity.platform.PlatformAccountAction;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.riders.RiderAccountStatus;
import com.equestrian.equis.repository.accounts.AdminAccountRepository;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.club.ClubRepository;
import com.equestrian.equis.repository.platform.PlatformAccountActionRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.service.accounts.AccountLinkCreatedEvent;
import com.equestrian.equis.service.accounts.AccountLinkService;
import com.equestrian.equis.service.accounts.AccountLinkService.IssuedLink;
import com.equestrian.equis.service.accounts.LinkSender;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

// What the super admin does to one account of a club from the platform's Accounts tab. Today: reset password.
//
// Two transactions, because one transaction works for one club only (TenantTransactions):
//   1. root mode: the club exists (any state: a suspended or blocked club can still be helped)
//   2. in the club: the account (by id and club, locked), its new link and the action row; the email goes after
//      this one commits (AccountLinkMailListener), so a refused or rolled-back reset writes and sends nothing.
@Service
public class PlatformAccountActionService {
    static final String RESET_PASSWORD = "reset_password";

    private final ClubRepository clubs;
    private final AdminAccountRepository admins;
    private final TrainerRepository trainers;
    private final RiderRepository riders;
    private final PlatformAccountActionRepository actions;
    private final AccountLinkService links;
    private final ApplicationEventPublisher events;
    private final TenantTransactions tenants;
    private final Clock clock;

    public PlatformAccountActionService(ClubRepository clubs, AdminAccountRepository admins,
                                        TrainerRepository trainers, RiderRepository riders,
                                        PlatformAccountActionRepository actions, AccountLinkService links,
                                        ApplicationEventPublisher events, TenantTransactions tenants, Clock clock) {
        this.clubs = clubs;
        this.admins = admins;
        this.trainers = trainers;
        this.riders = riders;
        this.actions = actions;
        this.links = links;
        this.events = events;
        this.tenants = tenants;
        this.clock = clock;
    }

    // A 24-hour set-password link for an active account; the account's older open link stops working. The old
    // password works until the link is used. Call outside a transaction: each step opens its own.
    public PlatformAccountReset resetPassword(UUID clubId, AccountType type, UUID accountId, UUID superAdminId) {
        Club club = tenants.asRoot(() -> clubs.findById(clubId).orElseThrow(ClubErrors::notFound));
        return tenants.inClub(clubId, () -> switch (type) {
            case ADMIN -> resetAdmin(club, accountId, superAdminId);
            case TRAINER -> resetTrainer(clubId, accountId, superAdminId);
            case RIDER -> resetRider(clubId, accountId, superAdminId);
        });
    }

    private PlatformAccountReset resetAdmin(Club club, UUID id, UUID superAdminId) {
        AdminAccount admin = admins.findByIdAndClubIdForUpdate(id, club.getId())
                .orElseThrow(PlatformAccountActionService::notFound);
        if (admin.getStatus() != AdminStatus.ACTIVE) {
            throw invalidStatus(PlatformAccountStatus.INVITED);
        }
        IssuedLink link = links.issue(admin, InvitePurpose.RESET);
        record(club.getId(), AccountType.ADMIN, admin.getId(), admin.getName(), admin.getEmail(), superAdminId);
        events.publishEvent(new AccountLinkCreatedEvent(SubjectType.ADMIN, admin.getId(), admin.getName(),
                admin.getEmail(), InvitePurpose.RESET, link.rawToken(), link.expiresAt(),
                club.getName(), club.getCode(), LinkSender.PLATFORM));
        return new PlatformAccountReset(admin.getId(), AccountType.ADMIN, admin.getEmail(), link.expiresAt());
    }

    private PlatformAccountReset resetTrainer(UUID clubId, UUID id, UUID superAdminId) {
        Trainer trainer = trainers.findByIdAndClubIdForUpdate(id, clubId)
                .orElseThrow(PlatformAccountActionService::notFound);
        if (trainer.getStatus() != TrainerStatus.ACTIVE) {
            throw invalidStatus(trainer.getStatus() == TrainerStatus.DISABLED
                    ? PlatformAccountStatus.DISABLED : PlatformAccountStatus.INVITED);
        }
        IssuedLink link = links.issue(trainer, InvitePurpose.RESET);
        record(clubId, AccountType.TRAINER, trainer.getId(), trainer.getName(), trainer.getEmail(), superAdminId);
        events.publishEvent(new AccountLinkCreatedEvent(SubjectType.TRAINER, trainer.getId(), trainer.getName(),
                trainer.getEmail(), InvitePurpose.RESET, link.rawToken(), link.expiresAt(), LinkSender.PLATFORM));
        return new PlatformAccountReset(trainer.getId(), AccountType.TRAINER, trainer.getEmail(), link.expiresAt());
    }

    private PlatformAccountReset resetRider(UUID clubId, UUID id, UUID superAdminId) {
        Rider rider = riders.findByIdAndClubIdForUpdate(id, clubId)
                .orElseThrow(PlatformAccountActionService::notFound);
        // An active rider always has an email: it is where their invite went.
        if (rider.getAccountStatus() != RiderAccountStatus.ACTIVE || rider.getEmail() == null) {
            throw invalidStatus(rider.getAccountStatus() == RiderAccountStatus.INVITED
                    ? PlatformAccountStatus.INVITED : PlatformAccountStatus.NO_ACCOUNT);
        }
        IssuedLink link = links.issue(rider, InvitePurpose.RESET);
        record(clubId, AccountType.RIDER, rider.getId(), rider.getName(), rider.getEmail(), superAdminId);
        events.publishEvent(new AccountLinkCreatedEvent(SubjectType.RIDER, rider.getId(), rider.getName(),
                rider.getEmail(), InvitePurpose.RESET, link.rawToken(), link.expiresAt(), LinkSender.PLATFORM));
        return new PlatformAccountReset(rider.getId(), AccountType.RIDER, rider.getEmail(), link.expiresAt());
    }

    private void record(UUID clubId, AccountType type, UUID accountId, String name, String email,
                        UUID superAdminId) {
        actions.save(PlatformAccountAction.record(clubId, type, accountId, name, email,
                AccountActionType.RESET_PASSWORD, superAdminId, clock.instant()));
    }

    // The same answer for an unknown id and another club's account, so the path never hints that it exists.
    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.ACCOUNT_NOT_FOUND, "This account doesn't exist.");
    }

    private static ApiException invalidStatus(PlatformAccountStatus status) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.INVALID_ACCOUNT_STATUS,
                "Only an active account can get a new password link.",
                Map.of("status", status.name(), "action", RESET_PASSWORD));
    }
}
