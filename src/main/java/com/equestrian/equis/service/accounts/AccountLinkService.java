package com.equestrian.equis.service.accounts;

import com.equestrian.equis.common.config.AuthProperties;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.entity.accounts.Invite;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.repository.accounts.InviteRepository;
import com.equestrian.equis.service.accounts.TokenGenerator.GeneratedToken;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

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
        // Only the newest link works.
        invites.deleteByTrainerIdAndAcceptedAtIsNull(trainer.getId());

        GeneratedToken generated = tokenGenerator.generate();
        Duration ttl = purpose == InvitePurpose.RESET ? properties.reset().ttl() : properties.invite().ttl();

        Invite invite = new Invite();
        invite.setTrainer(trainer);
        invite.setTokenHash(generated.hash());
        invite.setPurpose(purpose);
        invite.setExpiresAt(clock.instant().plus(ttl));
        invites.save(invite);
        return generated.raw();
    }

    // Every failure gives the same answer, so a caller cannot learn why a link was refused.
    @Transactional(readOnly = true)
    public Invite openLink(String raw) {
        if (raw == null || raw.isBlank()) {
            throw invalidLink();
        }
        Invite invite = invites.findByTokenHash(tokenGenerator.hash(raw))
                .orElseThrow(AccountLinkService::invalidLink);
        if (!invite.isOpen(clock.instant()) || invite.getTrainer().getStatus() == TrainerStatus.DISABLED) {
            throw invalidLink();
        }
        return invite;
    }

    // Called in the same transaction as saving the password, so a link is used only if the password was set.
    @Transactional
    public void markUsed(Invite invite) {
        invite.setAcceptedAt(clock.instant());
    }

    private static ApiException invalidLink() {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_LINK,
                "This link has expired or was already used. Ask your admin for a new one.");
    }
}
