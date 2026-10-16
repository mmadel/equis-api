package com.equestrian.equis.service.mail;

import com.equestrian.equis.service.accounts.AccountLinkCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Emails the link once the admin's action is committed; a rolled-back action sends nothing.
// No retry: on failure the link stays valid and the admin presses Resend.
@Component
public class AccountLinkMailListener {
    private static final Logger log = LoggerFactory.getLogger(AccountLinkMailListener.class);

    private final AccountLinkEmails emails;
    private final NotificationSender sender;

    public AccountLinkMailListener(AccountLinkEmails emails, NotificationSender sender) {
        this.emails = emails;
        this.sender = sender;
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(AccountLinkCreatedEvent e) {
        try {
            sender.send(emails.build(e));
        } catch (RuntimeException ex) {
            // Never the token or the body.
            log.warn("Account link email failed trainer={} purpose={}: {}", e.trainerId(), e.purpose(), ex.toString());
        }
    }
}
