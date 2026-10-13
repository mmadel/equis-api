package com.equestrian.equis.service.sessions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionGenerationJob {
    private static final Logger log = LoggerFactory.getLogger(SessionGenerationJob.class);

    private final SessionGenerator generator;

    public SessionGenerationJob(SessionGenerator generator) {
        this.generator = generator;
    }

    @Scheduled(cron = "${app.sessions.generate-cron}", zone = "${app.time-zone}")
    public void generateOnSchedule() {
        run();
    }

    // Catches up after downtime. A failure here must not stop the app from starting.
    @EventListener(ApplicationReadyEvent.class)
    public void generateOnStartup() {
        try {
            run();
        } catch (RuntimeException e) {
            log.error("Session generation on startup failed", e);
        }
    }

    private void run() {
        GenerationResult result = generator.generateAll();
        log.info("Generated {} sessions for {} templates", result.sessions(), result.templates());
    }
}
