package com.equestrian.equis.repository.payments;

import com.equestrian.equis.entity.payments.SessionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SessionUsageRepository extends JpaRepository<SessionUsage, UUID> {
    // Sessions used per package, in one query. Packages with no usage have no row.
    @Query("""
            select u.payment.id as paymentId, count(u) as used
            from SessionUsage u
            where u.payment.id in :paymentIds
            group by u.payment.id
            """)
    List<UsedCount> countUsedByPayment(@Param("paymentIds") Collection<UUID> paymentIds);
}
