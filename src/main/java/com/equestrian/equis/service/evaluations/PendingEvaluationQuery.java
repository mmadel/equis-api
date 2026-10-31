package com.equestrian.equis.service.evaluations;

import com.equestrian.equis.dto.evaluations.PendingEvaluations;
import com.equestrian.equis.repository.evaluations.SessionEvaluationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// What the trainer still has to evaluate, for Home. There is no deadline: an old finished session stays here
// until each of the trainer's riders in it has an evaluation.
@Service
public class PendingEvaluationQuery {
    private final SessionEvaluationRepository evaluations;

    public PendingEvaluationQuery(SessionEvaluationRepository evaluations) {
        this.evaluations = evaluations;
    }

    @Transactional(readOnly = true)
    public PendingEvaluations pending(UUID trainerId) {
        List<PendingEvaluations.PendingSession> sessions = evaluations.pendingForTrainer(trainerId).stream()
                .map(row -> new PendingEvaluations.PendingSession(row.sessionId(), row.date(), row.start(),
                        row.end(), row.levelId(), row.ridersToEvaluate().intValue()))
                .toList();
        long count = sessions.stream().mapToLong(PendingEvaluations.PendingSession::ridersToEvaluate).sum();
        return new PendingEvaluations(count, sessions);
    }
}
