package com.equestrian.equis.service.sessions;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionStatus;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.service.payments.NoSessionsLeftException;
import com.equestrian.equis.service.payments.SessionUsageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// The trainer marks a level session as finished: everyone seated in it uses one session, or nothing changes.
@Service
public class FinishSessionService {
    private final SessionRepository sessions;
    private final AssignmentRepository assignments;
    private final SessionUsageService usage;
    private final Clock clock;
    private final ZoneId schoolZone;

    public FinishSessionService(SessionRepository sessions, AssignmentRepository assignments,
                                SessionUsageService usage, Clock clock,
                                @Value("${app.time-zone}") ZoneId schoolZone) {
        this.sessions = sessions;
        this.assignments = assignments;
        this.usage = usage;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    // Finished once, for every coach's riders in it. The session row is locked first, then the riders' packages
    // (inside SessionUsageService); saving assignments locks the same row, so the seats can't change meanwhile.
    @Transactional
    public Session finish(UUID trainerId, UUID sessionId) {
        Instant now = clock.instant();
        Session session = sessions.findByIdForUpdate(sessionId)
                .filter(found -> !found.getTemplate().isAssessment())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.SESSION_NOT_FOUND,
                        "This session does not exist."));

        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.SESSION_READ_ONLY,
                    "This session was cancelled.");
        }
        if (session.isFinished()) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.SESSION_ALREADY_FINISHED,
                    "This session is already finished.");
        }
        if (!session.hasStarted(now, schoolZone)) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.SESSION_NOT_STARTED,
                    "This session has not started yet.");
        }

        List<Assignment> seats = assignments.findBySessionIdIn(List.of(sessionId));
        Set<UUID> mine = seats.stream()
                .filter(seat -> seat.getAssignedBy().getId().equals(trainerId))
                .map(seat -> seat.getRider().getId())
                .collect(Collectors.toSet());
        if (!SessionAccess.canFinish(session, now, schoolZone, mine.size(), seats.size())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.SESSION_NOT_YOURS,
                    "Only a trainer with a rider in this session can finish it.");
        }

        List<Rider> riders = seats.stream().map(Assignment::getRider).toList();
        try {
            usage.useOneSessionEach(session, riders, now);
        } catch (NoSessionsLeftException e) {
            throw noSessionsLeft(e.getRiders(), mine);
        }
        session.finish(trainerId, now);
        // Nobody to evaluate: an empty session is done at once.
        session.markDoneIfAllEvaluated(riders.size(), 0);
        return session;
    }

    // Other coaches' riders are only counted, as their names are not shown on the trainer's screen.
    private static ApiException noSessionsLeft(List<Rider> missing, Set<UUID> mine) {
        List<Rider> own = missing.stream().filter(rider -> mine.contains(rider.getId())).toList();
        int others = missing.size() - own.size();

        String message;
        if (!own.isEmpty()) {
            String names = joinNames(own.stream().map(Rider::getName).toList());
            message = names + (own.size() == 1 ? " has" : " have")
                    + " no sessions left. Remove them from the session, then finish it.";
        } else {
            message = others == 1
                    ? "1 rider of another coach has no sessions left. Ask their coach to remove them."
                    : others + " riders of another coach have no sessions left. Ask their coach to remove them.";
        }

        List<Map<String, Object>> ownRows = own.stream()
                .<Map<String, Object>>map(rider -> Map.of("id", rider.getId(), "name", rider.getName()))
                .toList();
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.RIDER_NO_SESSIONS_LEFT, message,
                Map.of("details", Map.of("riders", ownRows, "otherCoachRiders", others)));
    }

    // "A", "A and B", "A, B and C".
    private static String joinNames(List<String> names) {
        if (names.size() == 1) {
            return names.getFirst();
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.getLast();
    }
}
