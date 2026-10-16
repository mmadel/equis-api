package com.equestrian.equis.service.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Development only: prints the email, link included, instead of sending it.
@Component
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "log", matchIfMissing = true)
public class LogNotificationSender implements NotificationSender {
    private static final Logger log = LoggerFactory.getLogger(LogNotificationSender.class);

    @Override
    public void send(OutgoingEmail email) {
        log.info("[mail:log] to={} subject={}\n{}", email.to(), email.subject(), email.text());
    }
}
