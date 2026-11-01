package com.equestrian.equis.service.accounts;

import com.equestrian.equis.common.config.AuthProperties;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.entity.accounts.Invite;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.repository.accounts.InviteRepository;
import com.equestrian.equis.service.accounts.TokenGenerator.GeneratedToken;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class AccountLinkService {
    private final InviteRepository invites;
    private final TokenGenerator tokenGenerator;
    private final AuthProperties properties;
    private final Clock clock;

    public AccountLinkService(InviteRepository invites, TokenGenerator tokenGenerator,
                              AuthProperties properties, Clock clock) {
        this.invites = invites;
        this.tokenGenerator = tokenGenerator;
        this.properties = properties;
        this.clock = clock;
    }

    // Returns the raw token for the email. Only its hash is stored, so it cannot be read back later.
    @Transactional
    public String create(Trainer trainer, InvitePurpose purpose) {
        return issue(trainer, purpose).rawToken();
    }

    // Like create, plus when the link expires, for the email.
    @Transactional
    public IssuedLink issue(Trainer trainer, InvitePurpose purpose) {
        // Only the newest link works.
        invites.deleteByTrainerIdAndAcceptedAtIsNull(trainer.getId());

        GeneratedToken generated = tokenGenerator.generate();

        Invite invite = Invite.forTrainer(trainer);
        invite.setTokenHash(generated.hash());
        invite.setPurpose(purpose);
        invite.setExpiresAt(clock.instant().plus(ttl(purpose)));
        invites.save(invite);
        return new IssuedLink(generated.raw(), invite.getExpiresAt());
    }

    // A rider's link: same rules as a trainer's (only the newest works, 72h invite / 24h reset).
    @Transactional
    public IssuedLink issue(Rider rider, InvitePurpose purpose) {
        invites.deleteByRiderIdAndAcceptedAtIsNull(rider.getId());
        GeneratedToken generated = tokenGenerator.generate();
        Invite invite = Invite.forRider(rider);
        invite.setTokenHash(generated.hash());
        invite.setPurpose(purpose);
        invite.setExpiresAt(clock.instant().plus(ttl(purpose)));
        invites.save(invite);
        return new IssuedLink(generated.raw(), invite.getExpiresAt());
    }

    // Every failure gives the same answer, so a caller cannot learn why a link was refused.
    @Transactional(readOnly = true)
    public Invite openLink(String raw) {
        if (raw == null || raw.isBlank()) {
            throw invalidLink();
        }
        Invite invite = invites.findByTokenHash(tokenGenerator.hash(raw))
                .orElseThrow(AccountLinkService::invalidLink);
        // Riders have no disabled state, so their link only has to be unused and not expired.
        if (!invite.isOpen(clock.instant())
                || (invite.isForTrainer() && invite.getTrainer().getStatus() == TrainerStatus.DISABLED)) {
            throw invalidLink();
        }
        return invite;
    }

    // Called in the same transaction as saving the password, so a link is used only if the password was set.
    @Transactional
    public void markUsed(Invite invite) {
        invite.setAcceptedAt(clock.instant());
    }

    public record IssuedLink(String rawToken, Instant expiresAt) {
        // Keeps the token out of logs if the record is ever printed.
        @Override
        public String toString() {
            return "IssuedLink[expiresAt=" + expiresAt + "]";
        }
    }

    private Duration ttl(InvitePurpose purpose) {
        return purpose == InvitePurpose.RESET ? properties.reset().ttl() : properties.invite().ttl();
    }

    private static ApiException invalidLink() {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_LINK,
                "This link has expired or was already used. Ask your admin for a new one.");
    }
}
