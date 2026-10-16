package com.equestrian.equis.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableConfigurationProperties(MailSettings.class)
public class MailConfig {

    // A broken mail setup is found at startup, not at the first invite.
    public MailConfig(MailSettings settings,
                      @Value("${spring.mail.username:}") String username,
                      @Value("${spring.mail.password:}") String password) {
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
