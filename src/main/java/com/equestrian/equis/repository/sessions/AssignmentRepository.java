package com.equestrian.equis.repository.sessions;

import com.equestrian.equis.entity.sessions.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    // One query for a whole list of sessions, with each rider loaded, so listing seats is not N+1.
    @Query("""
            select a from Assignment a
            join fetch a.rider
            where a.session.id in :sessionIds
            """)
    List<Assignment> findBySessionIdIn(@Param("sessionIds") Collection<UUID> sessionIds);

    long countBySessionId(UUID sessionId);

    // Bulk delete: assignments already loaded in the persistence context are not refreshed.
    @Modifying
    @Query("delete from Assignment a where a.session.id in :sessionIds")
    int deleteBySessionIdIn(@Param("sessionIds") Collection<UUID> sessionIds);

    // The seat count of each upcoming scheduled session of the template; sessions without seats are absent.
    @Query("""
            select count(a) from Assignment a
            where a.session.template.id = :templateId
              and a.session.status = com.equestrian.equis.entity.sessions.SessionStatus.SCHEDULED
              and (a.session.sessionDate > :today
                   or (a.session.sessionDate = :today and a.session.startTime > :nowTime))
            group by a.session.id
            """)
    List<Long> countPerUpcomingSession(@Param("templateId") UUID templateId,
                                       @Param("today") LocalDate today,
                                       @Param("nowTime") LocalTime nowTime);
}
