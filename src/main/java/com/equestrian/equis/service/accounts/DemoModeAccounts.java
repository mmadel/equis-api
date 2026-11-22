package com.equestrian.equis.service.accounts;

import com.equestrian.equis.common.config.MailSettings;
import com.equestrian.equis.entity.accounts.AdminAccount;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.repository.accounts.InviteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Demo mode (app.mail.enabled=false): instead of a link and an email, an invite or reset sets the demo password at
// once, with the same result as accepting the link (AccountPasswords). Every place that would send an account link
// asks isOn() first. The caller has already run its checks; this runs in its transaction and its club.
@Service
public class DemoModeAccounts {
    private final MailSettings mail;
    private final InviteRepository invites;
    private final AccountPasswords passwords;

    public DemoModeAccounts(MailSettings mail, InviteRepository invites, AccountPasswords passwords) {
        this.mail = mail;
        this.invites = invites;
        this.passwords = passwords;
    }

    public boolean isOn() {
        return !mail.enabled();
    }

    // The account becomes active with the demo password; a link sent before mail was turned off stops working.
    @Transactional(propagation = Propagation.MANDATORY)
    public void activate(Trainer trainer) {
        invites.deleteByTrainerIdAndAcceptedAtIsNull(trainer.getId());
        passwords.apply(trainer, InvitePurpose.INVITE, mail.demoPassword());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void activate(Rider rider) {
        invites.deleteByRiderIdAndAcceptedAtIsNull(rider.getId());
        passwords.apply(rider, InvitePurpose.INVITE, mail.demoPassword());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void activate(AdminAccount admin) {
        invites.deleteByAdminAccountIdAndAcceptedAtIsNull(admin.getId());
        passwords.apply(admin, InvitePurpose.INVITE, mail.demoPassword());
    }

    // The demo password replaces the old one at once, and the person is signed out everywhere.
    @Transactional(propagation = Propagation.MANDATORY)
    public void resetPassword(Trainer trainer) {
        invites.deleteByTrainerIdAndAcceptedAtIsNull(trainer.getId());
        passwords.apply(trainer, InvitePurpose.RESET, mail.demoPassword());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void resetPassword(Rider rider) {
        invites.deleteByRiderIdAndAcceptedAtIsNull(rider.getId());
        passwords.apply(rider, InvitePurpose.RESET, mail.demoPassword());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void resetPassword(AdminAccount admin) {
        invites.deleteByAdminAccountIdAndAcceptedAtIsNull(admin.getId());
        passwords.apply(admin, InvitePurpose.RESET, mail.demoPassword());
    }
}
