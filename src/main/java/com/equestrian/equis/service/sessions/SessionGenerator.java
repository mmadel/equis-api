package com.equestrian.equis.service.sessions;

import com.equestrian.equis.common.config.SessionProperties;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Service
public class SessionGenerator {
    private static final Logger log = LoggerFactory.getLogger(SessionGenerator.class);

    private final SessionRepository sessions;
    private final SessionTemplateRepository templates;
    private final SessionProperties properties;
    private final Clock clock;
    private final TransactionTemplate newTransaction;

    public SessionGenerator(SessionRepository sessions, SessionTemplateRepository templates,
                            SessionProperties properties, Clock clock,
                            PlatformTransactionManager transactionManager) {
        this.sessions = sessions;
        this.templates = templates;
        this.properties = properties;
        this.clock = clock;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // Inserts the template's missing sessions from today to the end of the horizon; returns how many were added.
    // Joins the caller's transaction, so a template create or edit and its sessions commit together.
    @Transactional
    public int generateFor(SessionTemplate t) {
        if (t.isDeleted()) {
            return 0;
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate to = today.plusWeeks(properties.horizonWeeks()).minusDays(1);
        LocalDate from = today.isAfter(t.getValidFrom()) ? today : t.getValidFrom();
        if (t.getValidTo() != null && t.getValidTo().isBefore(to)) {
            to = t.getValidTo();
        }

        int inserted = 0;
        for (LocalDate date = from.with(TemporalAdjusters.nextOrSame(t.getDayOfWeek()));
             !date.isAfter(to); date = date.plusWeeks(1)) {
            inserted += sessions.insertIfAbsent(t.getId(), date, t.getStartTime(), t.getEndTime(), t.getCapacity());
        }
        return inserted;
    }

    // Each template runs in its own transaction, so one bad template does not stop the others.
    public GenerationResult generateAll() {
        int inserted = 0;
        int done = 0;
        for (SessionTemplate t : templates.findByDeletedAtIsNull()) {
            try {
                inserted += newTransaction.execute(status -> generateFor(t));
                done++;
            } catch (RuntimeException e) {
                log.error("Session generation failed for template {}", t.getId(), e);
            }
        }
        return new GenerationResult(inserted, done);
    }
}
