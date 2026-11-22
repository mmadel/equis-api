package com.equestrian.equis.service.accounts;

import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.accounts.AdminAccount;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.riders.Rider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Sets an account's password the way an accepted link does: an INVITE makes the account active, a RESET signs the
// person out everywhere. Used by /auth/invite/accept and by demo mode (DemoModeAccounts), so both give the same result.
// The password is hashed per account, so each gets its own salt.
@Service
public class AccountPasswords {
    private final PasswordService passwords;
    private final AuthTokenService tokens;

    public AccountPasswords(PasswordService passwords, AuthTokenService tokens) {
        this.passwords = passwords;
        this.tokens = tokens;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void apply(Trainer trainer, InvitePurpose purpose, String rawPassword) {
        trainer.setPasswordHash(passwords.hash(rawPassword));
        // A reset leaves an active trainer active; a disabled trainer never gets here.
        if (trainer.getStatus() == TrainerStatus.INVITED) {
            trainer.setStatus(TrainerStatus.ACTIVE);
        }
        if (purpose == InvitePurpose.RESET) {
            // Sign out everywhere, so a lost phone or leaked password cannot keep a session.
            tokens.revokeAll(Role.TRAINER, trainer.getId());
        }
    }

    // invited -> active, or a new password for an active rider.
    @Transactional(propagation = Propagation.MANDATORY)
    public void apply(Rider rider, InvitePurpose purpose, String rawPassword) {
        rider.setPassword(passwords.hash(rawPassword));
        if (purpose == InvitePurpose.RESET) {
            tokens.revokeAll(Role.RIDER, rider.getId());
        }
    }

    // invited -> active, or a new password for an active club admin.
    @Transactional(propagation = Propagation.MANDATORY)
    public void apply(AdminAccount admin, InvitePurpose purpose, String rawPassword) {
        admin.setPassword(passwords.hash(rawPassword));
        if (purpose == InvitePurpose.RESET) {
            tokens.revokeAll(Role.ADMIN, admin.getId());
        }
    }
}
