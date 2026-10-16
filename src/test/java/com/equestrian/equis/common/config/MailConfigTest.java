package com.equestrian.equis.common.config;

import com.equestrian.equis.service.mail.LogNotificationSender;
import com.equestrian.equis.service.mail.NotificationSender;
import com.equestrian.equis.service.mail.SmtpNotificationSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

// Which sender runs, and the startup check in smtp mode. No database needed.
public class MailConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
            .withUserConfiguration(MailConfig.class, LogNotificationSender.class, SmtpNotificationSender.class)
            .withPropertyValues("spring.mail.host=localhost");

    @Test
    void smtpModeWithBlankPasswordFailsAtStartup() {
        runner.withPropertyValues("app.mail.mode=smtp", "app.mail.from=school@example.com",
                        "spring.mail.username=school@example.com", "spring.mail.password=")
                .run(context -> assertThat(context).getFailure()
                        .rootCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("APP_MAIL_USERNAME, APP_MAIL_PASSWORD and APP_MAIL_FROM are required "
                                + "when APP_MAIL_MODE=smtp"));
    }

    @Test
    void noModeMeansLogSender() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).getBean(NotificationSender.class).isInstanceOf(LogNotificationSender.class);
        });
    }
}
