package com.equestrian.equis.service.mail;

import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.service.accounts.AccountLinkCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// The invite and set-password email. One email for both; the subject and first lines change with the purpose.
@Component
public class AccountLinkEmails {
    private static final DateTimeFormatter EXPIRES = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ENGLISH);
    private static final String INVITE_SUBJECT = "You're invited to Riding Schedule";
    private static final String RESET_SUBJECT = "Set a new password for Riding Schedule";
    private static final String INVITE_INTRO = "Your riding school has invited you to the Riding Schedule app.";
    private static final String RESET_INTRO =
            "Your admin sent you a link to set a new password for the Riding Schedule app.";
    private static final String RESET_NOTE = "Your current password works until you set a new one.";
    private static final String SIGN_IN = "After that, sign in on the app with your phone number or this email.";
    private static final String IGNORE = "If you weren't expecting this email, you can ignore it.";

    private final String baseUrl;
    private final ZoneId schoolZone;

    public AccountLinkEmails(@Value("${app.base-url}") String baseUrl, @Value("${app.time-zone}") ZoneId schoolZone) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.schoolZone = schoolZone;
    }

    public OutgoingEmail build(AccountLinkCreatedEvent e) {
        boolean reset = e.purpose() == InvitePurpose.RESET;
        // The token is base64url, so it is safe in a path.
        String link = baseUrl + "/invite/" + e.rawToken();
        String expiryLine = "This link works once and expires on "
                + EXPIRES.format(e.expiresAt().atZone(schoolZone)) + " (Cairo time).";
        String intro = reset ? RESET_INTRO : INVITE_INTRO;

        StringBuilder text = new StringBuilder()
                .append("Hi ").append(e.trainerName()).append(",\n\n")
                .append(intro).append("\n\n")
                .append("Set your password: ").append(link).append("\n\n")
                .append(expiryLine).append('\n')
                .append(SIGN_IN).append('\n');
        if (reset) {
            text.append(RESET_NOTE).append('\n');
        }
        text.append('\n').append(IGNORE).append('\n');

        String href = HtmlUtils.htmlEscape(link);
        StringBuilder html = new StringBuilder()
                .append("<p>Hi ").append(HtmlUtils.htmlEscape(e.trainerName())).append(",</p>")
                .append("<p>").append(intro).append("</p>")
                .append("<p><a href=\"").append(href).append("\" style=\"display:inline-block;padding:10px 18px;")
                .append("background:#1f6f50;color:#ffffff;text-decoration:none;border-radius:6px;\">")
                .append("Set your password</a></p>")
                .append("<p>").append(expiryLine).append("<br>").append(SIGN_IN);
        if (reset) {
            html.append("<br>").append(RESET_NOTE);
        }
        html.append("</p><p>").append(HtmlUtils.htmlEscape(IGNORE)).append("</p>");

        return new OutgoingEmail(e.email(), reset ? RESET_SUBJECT : INVITE_SUBJECT, text.toString(), html.toString());
    }

    private static String stripTrailingSlash(String url) {
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
