package com.equestrian.equis.service.evaluations;

import com.equestrian.equis.dto.evaluations.EvaluationFollowUp;
import com.equestrian.equis.dto.evaluations.EvaluationFollowUp.FollowUpTotals;
import com.equestrian.equis.dto.evaluations.EvaluationFollowUp.FollowUpTrainer;
import com.equestrian.equis.dto.evaluations.EvaluationFollowUp.RiderRef;
import com.equestrian.equis.dto.evaluations.EvaluationFollowUp.UnevaluatedSession;
import com.equestrian.equis.dto.evaluations.EvaluationFollowUp.UnfinishedSession;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.evaluations.SessionEvaluation;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionStatus;
import com.equestrian.equis.repository.evaluations.SessionEvaluationRepository;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// The admin's Follow-up screen: for one date, the started level sessions nobody finished and the finished ones
// with riders still unevaluated, grouped by the coach who seated the riders. At most three queries.
@Service
public class EvaluationFollowUpQuery {
    private static final List<SessionStatus> OPEN = List.of(SessionStatus.SCHEDULED, SessionStatus.FINISHED);

    private final SessionRepository sessions;
    private final AssignmentRepository assignments;
    private final SessionEvaluationRepository evaluations;
    private final Clock clock;
    private final ZoneId schoolZone;

    public EvaluationFollowUpQuery(SessionRepository sessions, AssignmentRepository assignments,
                                   SessionEvaluationRepository evaluations, Clock clock,
                                   @Value("${app.time-zone}") ZoneId schoolZone) {
        this.sessions = sessions;
        this.assignments = assignments;
        this.evaluations = evaluations;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    public LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), schoolZone);
    }

    @Transactional(readOnly = true)
    public EvaluationFollowUp followUp(LocalDate date) {
        Instant now = clock.instant();
        // Sorted by start time, so every trainer's lists come out in time order.
        List<Session> started = sessions.findLevelSessionsOn(date, OPEN).stream()
                .filter(s -> s.hasStarted(now, schoolZone))
                .toList();
        if (started.isEmpty()) {
            return empty(date);
        }

        Map<UUID, List<Assignment>> seatsBySession = assignments.findWithCoachBySessionIdIn(
                        started.stream().map(Session::getId).toList()).stream()
                .collect(Collectors.groupingBy(a -> a.getSession().getId()));

        List<UUID> finishedIds = started.stream()
                .filter(s -> s.getStatus() == SessionStatus.FINISHED && seatsBySession.containsKey(s.getId()))
                .map(Session::getId)
                .toList();
        Set<SeatKey> evaluated = new HashSet<>();
        if (!finishedIds.isEmpty()) {
            for (SessionEvaluation e : evaluations.findBySessionIdIn(finishedIds)) {
                evaluated.add(new SeatKey(e.getSession().getId(), e.getRider().getId()));
            }
        }

        Map<UUID, TrainerLooseEnds> byTrainer = new LinkedHashMap<>();
        Set<UUID> notFinishedSessions = new HashSet<>();
        int ridersToEvaluate = 0;
        for (Session session : started) {
            List<Assignment> seats = seatsBySession.getOrDefault(session.getId(), List.of());
            Map<UUID, List<Assignment>> seatsByCoach = new LinkedHashMap<>();
            for (Assignment seat : seats) {
                seatsByCoach.computeIfAbsent(seat.getAssignedBy().getId(), id -> new ArrayList<>()).add(seat);
            }
            for (List<Assignment> coachSeats : seatsByCoach.values()) {
                Trainer coach = coachSeats.getFirst().getAssignedBy();
                if (session.getStatus() == SessionStatus.SCHEDULED) {
                    looseEnds(byTrainer, coach).notFinished.add(new UnfinishedSession(session.getId(),
                            session.getStartTime(), session.getEndTime(), levelId(session), coachSeats.size()));
                    notFinishedSessions.add(session.getId());
                } else {
                    List<RiderRef> riders = coachSeats.stream()
                            .filter(a -> !evaluated.contains(new SeatKey(session.getId(), a.getRider().getId())))
                            .map(a -> new RiderRef(a.getRider().getId(), a.getRider().getName()))
                            .sorted(Comparator.comparing(RiderRef::name).thenComparing(RiderRef::riderId))
                            .toList();
                    if (!riders.isEmpty()) {
                        looseEnds(byTrainer, coach).toEvaluate.add(new UnevaluatedSession(session.getId(),
                                session.getStartTime(), session.getEndTime(), levelId(session), riders));
                        ridersToEvaluate += riders.size();
                    }
                }
            }
        }

        List<FollowUpTrainer> trainers = byTrainer.values().stream()
                .sorted(Comparator.comparing((TrainerLooseEnds t) -> t.trainer.getName())
                        .thenComparing(t -> t.trainer.getId()))
                .map(TrainerLooseEnds::toDto)
                .toList();
        return new EvaluationFollowUp(date,
                new FollowUpTotals(trainers.size(), notFinishedSessions.size(), ridersToEvaluate), trainers);
    }

    private static EvaluationFollowUp empty(LocalDate date) {
        return new EvaluationFollowUp(date, new FollowUpTotals(0, 0, 0), List.of());
    }

    private static TrainerLooseEnds looseEnds(Map<UUID, TrainerLooseEnds> byTrainer, Trainer coach) {
        return byTrainer.computeIfAbsent(coach.getId(), id -> new TrainerLooseEnds(coach));
    }

    private static int levelId(Session session) {
        return session.getTemplate().getLevel().getId();
    }

    private record SeatKey(UUID sessionId, UUID riderId) {
    }

    private static final class TrainerLooseEnds {
        private final Trainer trainer;
        private final List<UnfinishedSession> notFinished = new ArrayList<>();
        private final List<UnevaluatedSession> toEvaluate = new ArrayList<>();

        private TrainerLooseEnds(Trainer trainer) {
            this.trainer = trainer;
        }

        private FollowUpTrainer toDto() {
            return new FollowUpTrainer(trainer.getId(), trainer.getName(),
                    trainer.getStatus() == TrainerStatus.ACTIVE, List.copyOf(notFinished), List.copyOf(toEvaluate));
        }
    }
}
