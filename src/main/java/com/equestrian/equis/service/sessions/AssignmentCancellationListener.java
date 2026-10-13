package com.equestrian.equis.service.sessions;

import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// Frees the seats of cancelled sessions.
// Synchronous, so it runs inside the transaction that cancelled them: if it fails, the cancellation rolls back too.
@Component
public class AssignmentCancellationListener {
    private static final Logger log = LoggerFactory.getLogger(AssignmentCancellationListener.class);

    private final AssignmentRepository assignments;

    public AssignmentCancellationListener(AssignmentRepository assignments) {
        this.assignments = assignments;
    }

    @EventListener
    public void onSessionsCancelled(SessionsCancelledEvent event) {
        List<Assignment> lost = assignments.findBySessionIdIn(event.sessionIds());
        if (lost.isEmpty()) {
            return;
        }
        // Logged until the Notice listener exists to tell these trainers.
        Set<UUID> trainerIds = affectedTrainerIds(lost);
        assignments.deleteBySessionIdIn(event.sessionIds());
        log.info("Removed {} assignments from {} cancelled sessions of template {}; affected trainers: {}",
                lost.size(), event.sessionIds().size(), event.templateId(), trainerIds);
    }

    // The distinct trainers whose riders lost a seat. An unassigned rider has no trainer to tell.
    static Set<UUID> affectedTrainerIds(List<Assignment> lost) {
        Set<UUID> trainerIds = new LinkedHashSet<>();
        for (Assignment assignment : lost) {
            Trainer trainer = assignment.getRider().getTrainer();
            if (trainer != null) {
                trainerIds.add(trainer.getId());
            }
        }
        return trainerIds;
    }
}
