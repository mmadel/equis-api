package com.equestrian.equis.repository.evaluations;

import com.equestrian.equis.entity.evaluations.SessionEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SessionEvaluationRepository extends JpaRepository<SessionEvaluation, UUID> {
    long countBySessionId(UUID sessionId);

    boolean existsBySessionIdAndRiderId(UUID sessionId, UUID riderId);

    // One query for a whole list of sessions, with rider and trainer loaded. Don't call with an empty list.
    @Query("""
            select e from SessionEvaluation e
            join fetch e.rider
            join fetch e.trainer
            where e.session.id in :sessionIds
            """)
    List<SessionEvaluation> findBySessionIdIn(@Param("sessionIds") Collection<UUID> sessionIds);

    // The trainer's seats still to evaluate, one row per finished session, any date, newest session first.
    // A done session has every seat evaluated, so only finished ones can appear.
    @Query("""
            select new com.equestrian.equis.repository.evaluations.PendingEvaluationRow(
                       s.id, s.sessionDate, s.startTime, s.endTime, l.id, count(a))
            from Assignment a
            join a.session s
            join s.template t
            join t.level l
            where a.assignedBy.id = :trainerId
              and s.status = com.equestrian.equis.entity.sessions.SessionStatus.FINISHED
              and not exists (select 1 from SessionEvaluation e
                              where e.session.id = s.id and e.rider.id = a.rider.id)
            group by s.id, s.sessionDate, s.startTime, s.endTime, l.id
            order by s.sessionDate desc, s.startTime desc
            """)
    List<PendingEvaluationRow> pendingForTrainer(@Param("trainerId") UUID trainerId);
}
