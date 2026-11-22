package com.equestrian.equis.common.config;

import com.equestrian.equis.service.accounts.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableAsync
@EnableConfigurationProperties(MailSettings.class)
public class MailConfig {
    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    // A broken mail setup is found at startup, not at the first invite.
    public MailConfig(MailSettings settings,
                      @Value("${spring.mail.username:}") String username,
                      @Value("${spring.mail.password:}") String password) {
        if (!settings.enabled()) {
            // No email goes out, so the SMTP values are not needed; the demo password is.
            checkDemoPassword(settings.demoPassword());
            log.warn("Mail is turned off (EQUIS_MAIL_ENABLED=false): invites and password resets set the demo "
                    + "password and send no email. Demo use only.");
            return;
        }
        if (settings.mode() == MailSettings.Mode.SMTP
                && (isBlank(username) || isBlank(password) || isBlank(settings.from()))) {
            throw new IllegalStateException(
                    "APP_MAIL_USERNAME, APP_MAIL_PASSWORD and APP_MAIL_FROM are required when APP_MAIL_MODE=smtp");
        }
    }

    // Emails go out on their own threads, so the admin's request does not wait for the mail server.
    @Bean("mailExecutor")
    public ThreadPoolTaskExecutor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mail-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }

    // The same 8–72 byte rule as a password set through a link. The messages never include the value.
    private static void checkDemoPassword(String demoPassword) {
        if (isBlank(demoPassword)) {
            throw new IllegalStateException("EQUIS_DEMO_PASSWORD is required when EQUIS_MAIL_ENABLED=false");
        }
        int bytes = demoPassword.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < PasswordService.MIN_BYTES || bytes > PasswordService.MAX_BYTES) {
            throw new IllegalStateException("EQUIS_DEMO_PASSWORD must be 8 to 72 bytes");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
