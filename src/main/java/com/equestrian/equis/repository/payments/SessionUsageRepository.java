package com.equestrian.equis.repository.payments;

import com.equestrian.equis.entity.payments.SessionUsage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SessionUsageRepository extends JpaRepository<SessionUsage, UUID> {
    // Sessions used from one package.
    long countByPaymentId(UUID paymentId);

    // Sessions used per package, in one query. Packages with no usage have no row.
    @Query("""
            select u.payment.id as paymentId, count(u) as used
            from SessionUsage u
            where u.payment.id in :paymentIds
            group by u.payment.id
            """)
    List<UsedCount> countUsedByPayment(@Param("paymentIds") Collection<UUID> paymentIds);

    // Every session used from these packages, in one query, oldest first. The trainer is the one who seated the rider
    // in that session (null if that seat is gone), not the rider's trainer today.
    @Query("""
            select new com.equestrian.equis.repository.payments.UsageLine(
                u.payment.id, s.id, s.sessionDate, s.startTime, s.endTime, l.id, t.name, u.usedAt)
            from SessionUsage u
            join u.session s
            join s.template tp
            left join tp.level l
            left join Assignment a on a.session = s and a.rider = u.rider
            left join a.assignedBy t
            where u.payment.id in :paymentIds
            order by s.sessionDate, s.startTime, u.usedAt
            """)
    List<UsageLine> findUsageLines(@Param("paymentIds") Collection<UUID> paymentIds);

    // The rider's rides, newest first, one page. The coach is who evaluated the rider, or who finished the session
    // when there is no evaluation yet.
    @Query("""
            select new com.equestrian.equis.repository.payments.RiderSessionRow(
                s.id, s.sessionDate, s.startTime, s.endTime, l.id, coalesce(et.name, ft.name),
                e.stars, e.comment, e.createdAt)
            from SessionUsage u
            join u.session s
            join s.template tp
            join tp.level l
            left join SessionEvaluation e on e.session = s and e.rider = u.rider
            left join e.trainer et
            left join Trainer ft on ft.id = s.finishedBy
            where u.rider.id = :riderId
            order by s.sessionDate desc, s.startTime desc, s.id
            """)
    List<RiderSessionRow> findRiderSessions(@Param("riderId") UUID riderId, Pageable pageable);

    long countByRiderId(UUID riderId);
}
