package com.equestrian.equis.support;

import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;

// Records emails instead of sending them, and sends them on the calling thread,
// so the email exists before the request returns.
@TestConfiguration
public class MailTestConfig {

    @Bean
    @Primary
    public RecordingNotificationSender recordingNotificationSender() {
        return new RecordingNotificationSender();
    }

    // Swaps the definition rather than adding a second "mailExecutor": bean overriding is off.
    @Bean
    public static BeanDefinitionRegistryPostProcessor syncMailExecutor() {
        return registry -> {
            registry.removeBeanDefinition("mailExecutor");
            registry.registerBeanDefinition("mailExecutor", new RootBeanDefinition(SyncTaskExecutor.class));
        };
    }
}
