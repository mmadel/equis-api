package com.equestrian.equis.service.mail;

import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.service.accounts.AccountLinkCreatedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountLinkEmailsTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private static final String TOKEN = "Zt7abc-DEF_123";
    // Mon 19 Oct 2026, 01:20 in Cairo (summer time, UTC+3).
    private static final Instant EXPIRES = Instant.parse("2026-10-18T22:20:00Z");

    private final AccountLinkEmails emails = new AccountLinkEmails("http://test.local", CAIRO);

    @Test
    void inviteEmailHasLinkAndExpiryInCairoTime() {
        OutgoingEmail email = emails.build(event("Mona", InvitePurpose.INVITE));

        assertThat(email.to()).isEqualTo("mona@example.com");
        assertThat(email.subject()).isEqualTo("You're invited to Riding Schedule");
        assertThat(email.text())
                .startsWith("Hi Mona,")
                .contains("Your riding school has invited you to the Riding Schedule app.")
                .contains("Set your password: http://test.local/invite/" + TOKEN)
                .contains("This link works once and expires on Mon 19 Oct, 01:20 (Cairo time).")
                .contains("If you weren't expecting this email, you can ignore it.")
                .doesNotContain("Your current password works");
        assertThat(email.html()).contains("href=\"http://test.local/invite/" + TOKEN + "\"");
    }

    @Test
    void resetEmailHasResetSubjectIntroAndPasswordNote() {
        OutgoingEmail email = emails.build(event("Ahmed", InvitePurpose.RESET));

        assertThat(email.subject()).isEqualTo("Set a new password for Riding Schedule");
        assertThat(email.text())
                .contains("Your admin sent you a link to set a new password for the Riding Schedule app.")
                .contains("Your current password works until you set a new one.")
                .doesNotContain("has invited you");
        assertThat(email.html()).contains("Your current password works until you set a new one.");
    }

    @Test
    void trailingSlashInBaseUrlGivesNoDoubleSlash() {
        OutgoingEmail email = new AccountLinkEmails("http://test.local/", CAIRO)
                .build(event("Mona", InvitePurpose.INVITE));

        assertThat(email.text()).contains("http://test.local/invite/" + TOKEN).doesNotContain("local//");
    }

    @Test
    void nameIsEscapedInHtmlOnly() {
        OutgoingEmail email = emails.build(event("<b>Mona</b>", InvitePurpose.INVITE));

        assertThat(email.html()).contains("&lt;b&gt;Mona&lt;/b&gt;").doesNotContain("<b>Mona</b>");
        assertThat(email.text()).startsWith("Hi <b>Mona</b>,");
    }

    @Test
    void toStringHasOnlyRecipientAndSubject() {
        OutgoingEmail email = emails.build(event("Mona", InvitePurpose.INVITE));

        assertThat(email.toString())
                .isEqualTo("OutgoingEmail[to=mona@example.com, subject=You're invited to Riding Schedule]")
                .doesNotContain(TOKEN);
    }

    private static AccountLinkCreatedEvent event(String name, InvitePurpose purpose) {
        return new AccountLinkCreatedEvent(UUID.randomUUID(), name, "mona@example.com", purpose, TOKEN, EXPIRES);
    }
}
