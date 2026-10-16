package com.equestrian.equis.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// app.mail.*: which NotificationSender runs, and the From address and name of every email.
@ConfigurationProperties(prefix = "app.mail")
public record MailSettings(Mode mode, String from, String fromName) {

    public enum Mode { SMTP, LOG }
}
