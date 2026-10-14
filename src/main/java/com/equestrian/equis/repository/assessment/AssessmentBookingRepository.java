package com.equestrian.equis.repository.assessment;

import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentBookingRepository extends JpaRepository<AssessmentBooking, UUID> {
    // Row lock held until the transaction ends, so two Saves of the same result run one after the other.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from AssessmentBooking b where b.id = :id")
    Optional<AssessmentBooking> findByIdForUpdate(@Param("id") UUID id);

    // A slot's bookings with their members, oldest first: the A3 / A4 lists in one query.
    @Query("""
            select b from AssessmentBooking b
            join fetch b.member
            where b.session.id = :sessionId
            order by b.createdAt
            """)
    List<AssessmentBooking> findBySessionId(@Param("sessionId") UUID sessionId);

    long countBySessionIdAndStatus(UUID sessionId, BookingStatus status);

    // With UPCOMING there is at most one: a member has one active booking.
    Optional<AssessmentBooking> findByMemberIdAndStatus(UUID memberId, BookingStatus status);

    List<AssessmentBooking> findBySessionIdInAndStatus(Collection<UUID> sessionIds, BookingStatus status);

    // The UPCOMING booking count of each upcoming scheduled session of the template; sessions without any are absent.
    @Query("""
            select count(b) from AssessmentBooking b
            where b.session.template.id = :templateId
              and b.status = com.equestrian.equis.entity.assessment.BookingStatus.UPCOMING
              and b.session.status = com.equestrian.equis.entity.sessions.SessionStatus.SCHEDULED
              and (b.session.sessionDate > :today
                   or (b.session.sessionDate = :today and b.session.startTime > :nowTime))
            group by b.session.id
            """)
    List<Long> countUpcomingPerUpcomingSession(@Param("templateId") UUID templateId,
                                               @Param("today") LocalDate today,
                                               @Param("nowTime") LocalTime nowTime);
}
