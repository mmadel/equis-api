package com.equestrian.equis.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// app.mail.*: which NotificationSender runs, and the From address and name of every email.
// enabled=false (demo instances only) sends no email at all: invites and resets set demoPassword instead, and mode
// is ignored.
@ConfigurationProperties(prefix = "app.mail")
public record MailSettings(Mode mode, String from, String fromName,
                           Boolean enabled, String demoPassword) {

    public enum Mode { SMTP, LOG }

    // Unset or empty (compose passes an unset EQUIS_MAIL_ENABLED as ""): mail stays on.
    public MailSettings {
        if (enabled == null) {
            enabled = Boolean.TRUE;
        }
    }

    // Never prints the demo password.
    @Override
    public String toString() {
        return "MailSettings[mode=" + mode + ", from=" + from + ", fromName=" + fromName
                + ", enabled=" + enabled + "]";
    }
}
