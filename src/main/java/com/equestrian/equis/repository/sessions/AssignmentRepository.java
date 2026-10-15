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
    // The rider's seats on or after from, with session, template and level loaded, by date and start (uses ix_assignment_rider).
    @Query("""
            select a from Assignment a
            join fetch a.session s
            join fetch s.template t
            left join fetch t.level
            where a.rider.id = :riderId and s.sessionDate >= :from
            order by s.sessionDate, s.startTime
            """)
    List<Assignment> findFromDateByRiderId(@Param("riderId") UUID riderId, @Param("from") LocalDate from);

    // Every seat of the trainer's riders in scheduled sessions on or after today, with the session loaded, in time order.
    // The caller drops sessions that already started today.
    @Query("""
            select a from Assignment a
            join fetch a.session s
            where a.rider.trainer.id = :trainerId
              and s.status = com.equestrian.equis.entity.sessions.SessionStatus.SCHEDULED
              and s.sessionDate >= :today
            order by s.sessionDate, s.startTime
            """)
    List<Assignment> findFromDateForTrainer(@Param("trainerId") UUID trainerId, @Param("today") LocalDate today);

    // One query for a whole list of sessions, with each rider loaded, so listing seats is not N+1.
    @Query("""
            select a from Assignment a
            join fetch a.rider
            where a.session.id in :sessionIds
            """)
    List<Assignment> findBySessionIdIn(@Param("sessionIds") Collection<UUID> sessionIds);

    // One row per level with a filled seat in a scheduled session from..to; assessment sessions are left out.
    @Query("""
            select t.level.id as levelId, count(a) as seatCount
            from Assignment a
            join a.session s
            join s.template t
            where t.level is not null
              and s.status = com.equestrian.equis.entity.sessions.SessionStatus.SCHEDULED
              and s.sessionDate between :from and :to
            group by t.level.id
            """)
    List<LevelSeatCount> countScheduledPerLevel(@Param("from") LocalDate from, @Param("to") LocalDate to);

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
