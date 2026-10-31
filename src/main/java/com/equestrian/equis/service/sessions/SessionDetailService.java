package com.equestrian.equis.service.sessions;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.service.payments.RiderBalance;
import com.equestrian.equis.service.payments.RiderBalanceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SessionDetailService {
    private final SessionRepository sessions;
    private final AssignmentRepository assignments;
    private final RiderRepository riders;
    private final RiderBalanceService balances;
    private final Clock clock;
    private final ZoneId schoolZone;

    public SessionDetailService(SessionRepository sessions, AssignmentRepository assignments, RiderRepository riders,
                                RiderBalanceService balances, Clock clock,
                                @Value("${app.time-zone}") ZoneId schoolZone) {
        this.sessions = sessions;
        this.assignments = assignments;
        this.riders = riders;
        this.balances = balances;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    // An assessment session is answered like a missing one. Cancelled and finished sessions are returned, read-only.
    // One balance lookup covers the pick list and the caller's seats, however many riders there are.
    @Transactional(readOnly = true)
    public SessionDetail get(UUID trainerId, UUID sessionId) {
        Session session = sessions.findWithLevelById(sessionId)
                .filter(found -> !found.getTemplate().isAssessment())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.SESSION_NOT_FOUND,
                        "This session does not exist."));

        Instant now = clock.instant();
        LocalDate today = now.atZone(schoolZone).toLocalDate();
        List<Assignment> seats = assignments.findBySessionIdIn(List.of(sessionId));
        List<UUID> seatedByMe = seats.stream()
                .filter(seat -> seat.getAssignedBy().getId().equals(trainerId))
                .map(seat -> seat.getRider().getId())
                .toList();

        boolean canAssign = SessionAccess.canAssign(session, today);
        boolean canFinish = SessionAccess.canFinish(session, now, schoolZone, seatedByMe.size(), seats.size());

        // On any other day nobody can be added, so the caller's riders are not even read.
        List<Rider> myRiders = canAssign
                ? riders.findByTrainerIdAndLevelIdOrderByName(trainerId, session.getTemplate().getLevel().getId())
                : List.of();
        Set<UUID> ids = new LinkedHashSet<>(seatedByMe);
        myRiders.forEach(rider -> ids.add(rider.getId()));
        Map<UUID, Integer> sessionsLeft = balances.balancesOn(ids, session.getSessionDate()).values().stream()
                .collect(Collectors.toMap(RiderBalance::riderId, RiderBalance::sessionsLeft));

        List<Rider> eligible = myRiders.stream()
                .filter(rider -> sessionsLeft.getOrDefault(rider.getId(), 0) > 0)
                .toList();
        return new SessionDetail(session, seats, eligible, sessionsLeft, canAssign, canFinish);
    }
}
