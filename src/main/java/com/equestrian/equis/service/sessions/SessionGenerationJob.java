package com.equestrian.equis.service.sessions;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.repository.club.ClubRepository;
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
    private final ClubRepository clubs;

    public SessionGenerationJob(SessionGenerator generator, ClubRepository clubs) {
        this.generator = generator;
        this.clubs = clubs;
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

    // Club by club, each in its own club, whatever its subscription state. One club failing doesn't stop the others.
    // club has no RLS, so listing the clubs needs no club.
    private void run() {
        for (Club club : clubs.findAll()) {
            try {
                GenerationResult result = TenantContext.runAs(club.getId(), generator::generateAll);
                log.info("Generated {} sessions for {} templates in club {}",
                        result.sessions(), result.templates(), club.getCode());
            } catch (RuntimeException e) {
                log.error("Session generation failed for club {}", club.getCode(), e);
            }
        }
    }
}
