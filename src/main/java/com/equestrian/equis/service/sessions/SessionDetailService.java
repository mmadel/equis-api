package com.equestrian.equis.service.sessions;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionStatus;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class SessionDetailService {
    private final SessionRepository sessions;
    private final AssignmentRepository assignments;
    private final RiderRepository riders;
    private final Clock clock;
    private final ZoneId schoolZone;

    public SessionDetailService(SessionRepository sessions, AssignmentRepository assignments, RiderRepository riders,
                                Clock clock, @Value("${app.time-zone}") ZoneId schoolZone) {
        this.sessions = sessions;
        this.assignments = assignments;
        this.riders = riders;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    // An assessment session is answered like a missing one. A cancelled session is returned, as read-only.
    @Transactional(readOnly = true)
    public SessionDetail get(UUID trainerId, UUID sessionId) {
        Session session = sessions.findWithLevelById(sessionId)
                .filter(found -> !found.getTemplate().isAssessment())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.SESSION_NOT_FOUND,
                        "This session does not exist."));

        List<Assignment> seats = assignments.findBySessionIdIn(List.of(sessionId));
        List<Rider> myRiders = riders.findByTrainerIdAndLevelIdOrderByName(trainerId,
                session.getTemplate().getLevel().getId());
        boolean readOnly = session.getStatus() != SessionStatus.SCHEDULED
                || !session.isUpcoming(clock.instant(), schoolZone);
        return new SessionDetail(session, seats, myRiders, readOnly);
    }
}
