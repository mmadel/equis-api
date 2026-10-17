package com.equestrian.equis.repository.sessions;

import com.equestrian.equis.entity.sessions.Session;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    // Scheduled sessions of the template that have not started yet.
    @Query("""
            select s from Session s
            where s.template.id = :templateId
              and s.status = com.equestrian.equis.entity.sessions.SessionStatus.SCHEDULED
              and (s.sessionDate > :today or (s.sessionDate = :today and s.startTime > :nowTime))
            order by s.sessionDate, s.startTime
            """)
    List<Session> findUpcomingByTemplate(@Param("templateId") UUID templateId,
                                         @Param("today") LocalDate today,
                                         @Param("nowTime") LocalTime nowTime);

    // Row lock held until the transaction ends, so two trainers cannot fill the last seats at once.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Session s where s.id = :id")
    Optional<Session> findByIdForUpdate(@Param("id") UUID id);

    // The session with its template and level loaded; the level is null for an assessment session.
    @Query("""
            select s from Session s
            join fetch s.template t
            left join fetch t.level
            where s.id = :id
            """)
    Optional<Session> findWithLevelById(@Param("id") UUID id);

    // Scheduled sessions of the level from..to (both included), each with its template loaded.
    @Query("""
            select s from Session s
            join fetch s.template t
            where t.level.id = :levelId
              and s.status = com.equestrian.equis.entity.sessions.SessionStatus.SCHEDULED
              and s.sessionDate between :from and :to
            order by s.sessionDate, s.startTime
            """)
    List<Session> findScheduledByLevel(@Param("levelId") Short levelId,
                                       @Param("from") LocalDate from,
                                       @Param("to") LocalDate to);

    // Scheduled sessions of every level from..to (both included), with template and level loaded.
    // The inner join on the level leaves assessment slots out.
    @Query("""
            select s from Session s
            join fetch s.template t
            join fetch t.level l
            where s.status = com.equestrian.equis.entity.sessions.SessionStatus.SCHEDULED
              and s.sessionDate between :from and :to
            order by s.sessionDate, s.startTime, l.id
            """)
    List<Session> findScheduledLevelSessionsBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // Scheduled assessment slots from..to (both included), each with its template loaded.
    @Query("""
            select s from Session s
            join fetch s.template t
            where t.level is null
              and s.status = com.equestrian.equis.entity.sessions.SessionStatus.SCHEDULED
              and s.sessionDate between :from and :to
            order by s.sessionDate, s.startTime
            """)
    List<Session> findAssessmentSlots(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // One row per level with a scheduled session from..to; assessment sessions are left out.
    @Query("""
            select t.level.id as levelId, count(s) as sessionCount, sum(s.capacity) as seatsTotal
            from Session s
            join s.template t
            where t.level is not null
              and s.status = com.equestrian.equis.entity.sessions.SessionStatus.SCHEDULED
              and s.sessionDate between :from and :to
            group by t.level.id
            """)
    List<LevelSessionCount> countScheduledPerLevel(@Param("from") LocalDate from, @Param("to") LocalDate to);

    boolean existsByTemplateIdAndSessionDate(UUID templateId, LocalDate sessionDate);

    // Returns 1 when the session was inserted, 0 when the template already has one on that date.
    @Modifying
    @Query(value = """
            INSERT INTO session (template_id, session_date, start_time, end_time, capacity)
            VALUES (:templateId, :sessionDate, :startTime, :endTime, :capacity)
            ON CONFLICT (template_id, session_date) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("templateId") UUID templateId,
                       @Param("sessionDate") LocalDate sessionDate,
                       @Param("startTime") LocalTime startTime,
                       @Param("endTime") LocalTime endTime,
                       @Param("capacity") int capacity);
}
