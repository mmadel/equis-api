package com.equestrian.equis.service.evaluations;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.evaluations.EvaluationRequest;
import com.equestrian.equis.entity.evaluations.SessionEvaluation;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionStatus;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.evaluations.SessionEvaluationRepository;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// A trainer evaluates one rider they seated in a finished session. Final: there is no edit.
// The last evaluation of the session, of any coach, makes it done.
@Service
public class EvaluationService {
    private final SessionRepository sessions;
    private final AssignmentRepository assignments;
    private final SessionEvaluationRepository evaluations;
    private final TrainerRepository trainers;
    private final Clock clock;

    public EvaluationService(SessionRepository sessions, AssignmentRepository assignments,
                             SessionEvaluationRepository evaluations, TrainerRepository trainers, Clock clock) {
        this.sessions = sessions;
        this.assignments = assignments;
        this.evaluations = evaluations;
        this.trainers = trainers;
        this.clock = clock;
    }

    // The session row is locked first, so two coaches saving the last two riders at once set done once, and two
    // saves for the same rider are answered one after the other. The unique constraint is the backstop.
    @Transactional
    public Session evaluate(UUID trainerId, UUID sessionId, EvaluationRequest req) {
        Instant now = clock.instant();
        Session session = sessions.findByIdForUpdate(sessionId)
                .filter(found -> !found.getTemplate().isAssessment())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.SESSION_NOT_FOUND,
                        "This session does not exist."));

        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.SESSION_READ_ONLY,
                    "This session was cancelled.");
        }
        if (!session.isFinished()) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.SESSION_NOT_FINISHED,
                    "This session is not finished yet.");
        }

        Assignment seat = assignments.findBySessionIdIn(List.of(sessionId)).stream()
                .filter(found -> found.getRider().getId().equals(req.riderId()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        ErrorCodes.RIDER_NOT_IN_SESSION, "This rider was not in this session.",
                        riderIdError("This rider was not in this session.")));
        // Whoever seated the rider evaluates them, whatever the rider's coach, level or status is today.
        if (!seat.getAssignedBy().getId().equals(trainerId)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCodes.RIDER_NOT_YOURS,
                    "Only the coach who seated this rider can evaluate them.",
                    riderIdError("This rider was seated by another coach."));
        }

        String name = seat.getRider().getName();
        if (evaluations.existsBySessionIdAndRiderId(sessionId, req.riderId())) {
            throw alreadyEvaluated(name);
        }
        try {
            evaluations.saveAndFlush(SessionEvaluation.of(session, seat.getRider(),
                    trainers.getReferenceById(trainerId), req.stars(), req.comment(), now));
        } catch (DataIntegrityViolationException e) {
            throw alreadyEvaluated(name);
        }

        session.markDoneIfAllEvaluated(assignments.countBySessionId(sessionId),
                evaluations.countBySessionId(sessionId));
        return session;
    }

    private static ApiException alreadyEvaluated(String riderName) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.ALREADY_EVALUATED,
                riderName + " already has an evaluation for this session.");
    }

    private static Map<String, Object> riderIdError(String message) {
        return Map.of("fieldErrors", List.of(new ApiFieldError("riderId", message)));
    }
}
