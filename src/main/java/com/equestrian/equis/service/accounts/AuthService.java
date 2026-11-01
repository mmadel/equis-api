package com.equestrian.equis.service.accounts;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.dto.accounts.InviteAcceptRequest;
import com.equestrian.equis.dto.accounts.LoginRequest;
import com.equestrian.equis.entity.accounts.AdminAccount;
import com.equestrian.equis.entity.accounts.Invite;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.repository.accounts.AdminAccountRepository;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {
    private final AdminAccountRepository admins;
    private final TrainerRepository trainers;
    private final RiderRepository riders;
    private final AdminLockoutService lockout;
    private final PasswordService passwords;
    private final AuthTokenService tokens;
    private final AccountLinkService links;
    private final Clock clock;

    public AuthService(AdminAccountRepository admins, TrainerRepository trainers, RiderRepository riders,
                       AdminLockoutService lockout, PasswordService passwords, AuthTokenService tokens,
                       AccountLinkService links, Clock clock) {
        this.admins = admins;
        this.trainers = trainers;
        this.riders = riders;
        this.lockout = lockout;
        this.passwords = passwords;
        this.tokens = tokens;
        this.links = links;
        this.clock = clock;
    }

    // noRollbackFor keeps the admin's failed-attempt counter when the 401 is thrown.
    @Transactional(noRollbackFor = ApiException.class)
    public AuthTokens login(LoginRequest request) {
        String login = request.login().trim();
        Optional<Trainer> found;

        if (login.contains("@")) {
            // Emails are stored lowercase.
            String email = login.toLowerCase(Locale.ROOT);
            // The row lock stops two wrong tries at the same moment from reading the same counter.
            Optional<AdminAccount> admin = admins.findByEmailForUpdate(email);
            if (admin.isPresent()) {
                lockout.verifyPassword(admin.get(), request.password());
                return tokens.issue(Role.ADMIN, admin.get().getId(), request.rememberMe());
            }
            found = trainers.findByEmail(email);
            // Riders sign in by email only. An email belongs to one kind of account, so at most one matches.
            if (found.isEmpty()) {
                Optional<Rider> rider = riders.findByEmail(email);
                if (rider.isPresent()) {
                    return riderLogin(rider.get(), request);
                }
            }
        } else {
            // Phones are stored without spaces: "010 0000 0001" -> "01000000001".
            found = trainers.findByPhone(login.replace(" ", ""));
        }

        // Null when there is no trainer, and also for an invited trainer who has not set a password yet.
        String hash = found.map(Trainer::getPasswordHash).orElse(null);
        // All three cases give the same answer and take the same time.
        if (!passwords.matches(request.password(), hash)) {
            throw invalidCredentials();
        }

        Trainer trainer = found.orElseThrow();
        // Checked after the password, so only someone who knows it learns the account is disabled.
        if (trainer.getStatus() == TrainerStatus.DISABLED) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.ACCOUNT_DISABLED,
                    "This account has been disabled.");
        }

        // The entity is managed, so this is saved when the transaction commits.
        trainer.setLastActiveAt(clock.instant());
        return tokens.issue(Role.TRAINER, trainer.getId(), request.rememberMe());
    }

    private AuthTokens riderLogin(Rider rider, LoginRequest request) {
        // Null for an invited rider who has not set a password yet: the same answer and time as a wrong password.
        if (!passwords.matches(request.password(), rider.getPasswordHash())) {
            throw invalidCredentials();
        }
        rider.touch(clock.instant());
        return tokens.issue(Role.RIDER, rider.getId(), request.rememberMe());
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.INVALID_CREDENTIALS,
                "Wrong email, phone or password.");
    }

    // If any step fails nothing is saved: the password, the status and the link all stay as they were.
    @Transactional
    public void acceptInvite(InviteAcceptRequest request) {
        // The link is checked first, so an expired link never gets a "password too short" message.
        Invite invite = links.openLink(request.token());
        if (invite.isForRider()) {
            acceptRiderLink(invite, request.password());
            return;
        }
        Trainer trainer = invite.getTrainer();

        trainer.setPasswordHash(passwords.hash(request.password()));
        // A reset link leaves an active trainer active; a disabled trainer was refused by openLink.
        if (trainer.getStatus() == TrainerStatus.INVITED) {
            trainer.setStatus(TrainerStatus.ACTIVE);
        }
        links.markUsed(invite);

        if (invite.getPurpose() == InvitePurpose.RESET) {
            // Sign out everywhere, so a lost phone or leaked password cannot keep a session.
            tokens.revokeAll(Role.TRAINER, trainer.getId());
        }
    }

    // invited -> active, or a new password for an active rider. A reset signs the rider out everywhere.
    private void acceptRiderLink(Invite invite, String rawPassword) {
        Rider rider = invite.getRider();
        rider.setPassword(passwords.hash(rawPassword));
        links.markUsed(invite);
        if (invite.getPurpose() == InvitePurpose.RESET) {
            tokens.revokeAll(Role.RIDER, rider.getId());
        }
    }
}
