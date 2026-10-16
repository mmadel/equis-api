package com.equestrian.equis.service.mail;

import com.equestrian.equis.common.config.MailConfig;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Address;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

// The smtp sender against an in-memory SMTP server (plain SMTP, so STARTTLS is turned off here).
public class SmtpNotificationSenderTest {
    private static final String FROM = "school@example.com";

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser(FROM, "test-password"));

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
            .withUserConfiguration(MailConfig.class, LogNotificationSender.class, SmtpNotificationSender.class)
            .withPropertyValues(
                    "app.mail.mode=smtp",
                    "app.mail.from=" + FROM,
                    "app.mail.from-name=Riding Schedule",
                    "spring.mail.host=127.0.0.1",
                    "spring.mail.port=" + ServerSetupTest.SMTP.getPort(),
                    "spring.mail.username=" + FROM,
                    "spring.mail.password=test-password",
                    "spring.mail.properties.mail.smtp.auth=true",
                    "spring.mail.properties.mail.smtp.starttls.enable=false",
                    "spring.mail.properties.mail.smtp.starttls.required=false");

    @Test
    void sendsOneMultipartMessage() {
        String link = "http://test.local/invite/Zt7abc";
        OutgoingEmail email = new OutgoingEmail("mona@example.com", "You're invited to Riding Schedule",
                "Set your password: " + link, "<p><a href=\"" + link + "\">Set your password</a></p>");

        runner.run(context -> {
            assertThat(context).getBean(NotificationSender.class).isInstanceOf(SmtpNotificationSender.class);
            context.getBean(NotificationSender.class).send(email);

            MimeMessage[] received = greenMail.getReceivedMessages();
            assertThat(received).hasSize(1);
            MimeMessage message = received[0];
            Address from = message.getFrom()[0];
            assertThat(((InternetAddress) from).getAddress()).isEqualTo(FROM);
            assertThat(((InternetAddress) from).getPersonal()).isEqualTo("Riding Schedule");
            assertThat(GreenMailUtil.getAddressList(message.getAllRecipients())).isEqualTo("mona@example.com");
            assertThat(message.getSubject()).isEqualTo("You're invited to Riding Schedule");
            assertThat(message.getContent()).isInstanceOf(MimeMultipart.class);
            String raw = GreenMailUtil.getWholeMessage(message);
            assertThat(raw).contains("text/plain").contains("text/html").contains(link);
        });
    }
}
