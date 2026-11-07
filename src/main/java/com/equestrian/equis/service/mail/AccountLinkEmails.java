package com.equestrian.equis.service.mail;

import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.SubjectType;
import com.equestrian.equis.service.accounts.AccountLinkCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// The invite and set-password email. One email for both; the subject and first lines change with the purpose.
// A trainer's link opens the trainer app (app.base-url), a rider's the rider app (app.rider-base-url), a club admin's
// the web console (app.admin-base-url). The admin's email also names the club and gives its code.
@Component
public class AccountLinkEmails {
    private static final DateTimeFormatter EXPIRES = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ENGLISH);
    private static final String RESET_SUBJECT = "Set a new password for Riding Schedule";
    private static final String RESET_NOTE = "Your current password works until you set a new one.";
    private static final String IGNORE = "If you weren't expecting this email, you can ignore it.";

    private static final Texts TRAINER = new Texts(
            "You're invited to Riding Schedule",
            "Your riding school has invited you to the Riding Schedule app.",
            "Your admin sent you a link to set a new password for the Riding Schedule app.",
            "After that, sign in on the app with your phone number or this email.");
    private static final Texts RIDER = new Texts(
            "Your rider account at Riding Schedule",
            "Your riding school has created your rider account. "
                    + "Set a password to see your sessions, balance and progress.",
            "Your admin sent you a link to set a new password for your rider account.",
            "After that, sign in on the rider app with this email.");
    // inviteIntro is a format: the club's name goes in.
    private static final Texts ADMIN = new Texts(
            "Your club admin account at Riding Schedule",
            "You're the admin of %s on Riding Schedule. Set a password to start.",
            "You asked for a link to set a new password for your club admin account.",
            "After that, sign in on the web console with this email and your club code.");

    private final String baseUrl;
    private final String riderBaseUrl;
    private final String adminBaseUrl;
    private final ZoneId schoolZone;

    public AccountLinkEmails(@Value("${app.base-url}") String baseUrl,
                             @Value("${app.rider-base-url}") String riderBaseUrl,
                             @Value("${app.admin-base-url}") String adminBaseUrl,
                             @Value("${app.time-zone}") ZoneId schoolZone) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.riderBaseUrl = stripTrailingSlash(riderBaseUrl);
        this.adminBaseUrl = stripTrailingSlash(adminBaseUrl);
        this.schoolZone = schoolZone;
    }

    public OutgoingEmail build(AccountLinkCreatedEvent e) {
        boolean admin = e.subjectType() == SubjectType.ADMIN;
        Texts texts = switch (e.subjectType()) {
            case RIDER -> RIDER;
            case ADMIN -> ADMIN;
            default -> TRAINER;
        };
        String appUrl = switch (e.subjectType()) {
            case RIDER -> riderBaseUrl;
            case ADMIN -> adminBaseUrl;
            default -> baseUrl;
        };
        boolean reset = e.purpose() == InvitePurpose.RESET;
        // The token is base64url, so it is safe in a path.
        String link = appUrl + "/invite/" + e.rawToken();
        String expiryLine = "This link works once and expires on "
                + EXPIRES.format(e.expiresAt().atZone(schoolZone)) + " (Cairo time).";
        String intro;
        String htmlIntro;
        if (reset) {
            intro = texts.resetIntro();
            htmlIntro = intro;
        } else if (admin) {
            // The club's name was typed by the super admin, so it is escaped in the HTML part.
            intro = texts.inviteIntro().formatted(e.clubName());
            htmlIntro = texts.inviteIntro().formatted(HtmlUtils.htmlEscape(e.clubName()));
        } else {
            intro = texts.inviteIntro();
            htmlIntro = intro;
        }
        String clubCodeLine = admin ? "Your club code: " + e.clubCode() : null;

        StringBuilder text = new StringBuilder()
                .append("Hi ").append(e.name()).append(",\n\n")
                .append(intro).append("\n\n")
                .append("Set your password: ").append(link).append("\n\n");
        if (clubCodeLine != null) {
            text.append(clubCodeLine).append('\n');
        }
        text.append(expiryLine).append('\n')
                .append(texts.signIn()).append('\n');
        if (reset) {
            text.append(RESET_NOTE).append('\n');
        }
        text.append('\n').append(IGNORE).append('\n');

        String href = HtmlUtils.htmlEscape(link);
        StringBuilder html = new StringBuilder()
                .append("<p>Hi ").append(HtmlUtils.htmlEscape(e.name())).append(",</p>")
                .append("<p>").append(htmlIntro).append("</p>")
                .append("<p><a href=\"").append(href).append("\" style=\"display:inline-block;padding:10px 18px;")
                .append("background:#1f6f50;color:#ffffff;text-decoration:none;border-radius:6px;\">")
                .append("Set your password</a></p>")
                .append("<p>");
        if (clubCodeLine != null) {
            html.append(HtmlUtils.htmlEscape(clubCodeLine)).append("<br>");
        }
        html.append(expiryLine).append("<br>").append(texts.signIn());
        if (reset) {
            html.append("<br>").append(RESET_NOTE);
        }
        html.append("</p><p>").append(HtmlUtils.htmlEscape(IGNORE)).append("</p>");

        return new OutgoingEmail(e.email(), reset ? RESET_SUBJECT : texts.inviteSubject(), text.toString(),
                html.toString());
    }

    private static String stripTrailingSlash(String url) {
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private record Texts(String inviteSubject, String inviteIntro, String resetIntro, String signIn) {
    }
}
