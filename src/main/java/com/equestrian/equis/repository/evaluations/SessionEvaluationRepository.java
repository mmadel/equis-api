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
}
