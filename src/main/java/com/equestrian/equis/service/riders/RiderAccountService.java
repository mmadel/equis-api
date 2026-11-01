package com.equestrian.equis.service.riders;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.riders.RiderAccount;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.SubjectType;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.riders.RiderAccountStatus;
import com.equestrian.equis.repository.accounts.AdminAccountRepository;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.service.accounts.AccountErrors;
import com.equestrian.equis.service.accounts.AccountLinkCreatedEvent;
import com.equestrian.equis.service.accounts.AccountLinkService;
import com.equestrian.equis.service.accounts.AccountLinkService.IssuedLink;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

// The rider's app account: the invite sent at placement, and W3's Send app invite and Reset app password.
// The email goes after commit (AccountLinkMailListener), so a rolled-back action sends nothing.
@Service
public class RiderAccountService {
    private final RiderRepository riders;
    private final TrainerRepository trainers;
    private final AdminAccountRepository admins;
    private final AccountLinkService links;
    private final ApplicationEventPublisher events;

    public RiderAccountService(RiderRepository riders, TrainerRepository trainers, AdminAccountRepository admins,
                               AccountLinkService links, ApplicationEventPublisher events) {
        this.riders = riders;
        this.trainers = trainers;
        this.admins = admins;
        this.links = links;
        this.events = events;
    }

    // Called in the placement's transaction, after the rider is saved. A rider with no email gets no invite.
    @Transactional
    public void inviteNewRider(Rider rider) {
        if (rider.getEmail() == null) {
            return;
        }
        sendInvite(rider);
    }

    // From none or invited; each call makes a new 72-hour link and the earlier one stops working.
    @Transactional
    public RiderAccount invite(UUID riderId) {
        Rider rider = riders.findByIdForUpdate(riderId).orElseThrow(RiderAccountService::notFound);
        if (rider.getEmail() == null) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.RIDER_NO_EMAIL, "This rider has no email.");
        }
        if (rider.getAccountStatus() == RiderAccountStatus.ACTIVE) {
            throw invalidStatus(rider, "invite");
        }
        // Old data only: intake refuses these emails since this release.
        if (trainers.existsByEmail(rider.getEmail()) || admins.existsByEmail(rider.getEmail())) {
            throw AccountErrors.emailInUse();
        }
        return sendInvite(rider);
    }

    // Only for an active rider: a 24-hour link. Nothing else changes until it is used; the old password still works.
    @Transactional
    public RiderAccount resetPassword(UUID riderId) {
        Rider rider = riders.findByIdForUpdate(riderId).orElseThrow(RiderAccountService::notFound);
        if (rider.getAccountStatus() != RiderAccountStatus.ACTIVE) {
            throw invalidStatus(rider, "reset-password");
        }
        return sendLink(rider, InvitePurpose.RESET);
    }

    private RiderAccount sendInvite(Rider rider) {
        rider.markInvited();
        return sendLink(rider, InvitePurpose.INVITE);
    }

    private RiderAccount sendLink(Rider rider, InvitePurpose purpose) {
        IssuedLink link = links.issue(rider, purpose);
        events.publishEvent(new AccountLinkCreatedEvent(SubjectType.RIDER, rider.getId(), rider.getName(),
                rider.getEmail(), purpose, link.rawToken(), link.expiresAt()));
        return new RiderAccount(rider.getId(), rider.getAccountStatus(),
                purpose.name().toLowerCase(Locale.ROOT), link.expiresAt());
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.RIDER_NOT_FOUND, "This rider does not exist.");
    }

    private static ApiException invalidStatus(Rider rider, String action) {
        String status = rider.getAccountStatus().json();
        String detail = switch (action) {
            case "invite" -> "This rider already has an account. Reset their app password instead.";
            default -> "This rider hasn't set a password yet. Send the app invite again instead.";
        };
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.INVALID_ACCOUNT_STATUS, detail,
                Map.of("status", status, "action", action));
    }
}
