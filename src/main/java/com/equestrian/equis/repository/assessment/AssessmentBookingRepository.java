package com.equestrian.equis.repository.assessment;

import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    // A slot's bookings with member, assessor and level, oldest first, cancelled ones left out: A3 / A4.
    // The id only breaks ties between bookings made in the same instant, so the order is stable.
    @Query("""
            select b from AssessmentBooking b
            join fetch b.member
            join fetch b.assessor
            left join fetch b.level
            where b.session.id = :sessionId
              and b.status <> com.equestrian.equis.entity.assessment.BookingStatus.CANCELLED
            order by b.createdAt, b.id
            """)
    List<AssessmentBooking> findVisibleBySessionId(@Param("sessionId") UUID sessionId);

    // The bookings that aren't cancelled, per slot; slots without any are absent. Don't call with an empty list.
    @Query("""
            select b.session.id as sessionId, count(b) as booked
            from AssessmentBooking b
            where b.session.id in :sessionIds
              and b.status <> com.equestrian.equis.entity.assessment.BookingStatus.CANCELLED
            group by b.session.id
            """)
    List<SlotBookedCount> countActiveBySessionIdIn(@Param("sessionIds") Collection<UUID> sessionIds);

    // Results each assessor recorded (placed or no-show) with from <= recordedAt < to; assessors without any are
    // absent. Don't call with an empty list.
    @Query("""
            select b.assessor.id as assessorId, count(b) as recorded
            from AssessmentBooking b
            where b.assessor.id in :assessorIds
              and b.status in (com.equestrian.equis.entity.assessment.BookingStatus.PLACED,
                               com.equestrian.equis.entity.assessment.BookingStatus.NOSHOW)
              and b.recordedAt >= :from and b.recordedAt < :to
            group by b.assessor.id
            """)
    List<AssessorCount> countRecordedByAssessorIdsBetween(@Param("assessorIds") Collection<UUID> assessorIds,
                                                          @Param("from") Instant from,
                                                          @Param("to") Instant to);

    long countBySessionIdAndStatus(UUID sessionId, BookingStatus status);

    // With UPCOMING: the assessor's bookings without a result yet (upcoming or waiting for result).
    long countByAssessorIdAndStatus(UUID assessorId, BookingStatus status);

    // With UPCOMING there is at most one: a member has one active booking.
    Optional<AssessmentBooking> findByMemberIdAndStatus(UUID memberId, BookingStatus status);

    // The booking a member was placed by, with assessor and level: the "Placed in Level N" line on 4a.
    // A member is placed at most once, so there is at most one.
    @Query("""
            select b from AssessmentBooking b
            join fetch b.assessor
            join fetch b.level
            where b.member.id = :memberId
              and b.status = com.equestrian.equis.entity.assessment.BookingStatus.PLACED
            """)
    Optional<AssessmentBooking> findPlacedByMemberId(@Param("memberId") UUID memberId);

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
