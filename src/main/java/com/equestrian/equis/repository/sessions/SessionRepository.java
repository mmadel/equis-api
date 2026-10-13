package com.equestrian.equis.repository.sessions;

import com.equestrian.equis.entity.sessions.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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

    boolean existsByTemplateIdAndSessionDate(UUID templateId, LocalDate sessionDate);
}
