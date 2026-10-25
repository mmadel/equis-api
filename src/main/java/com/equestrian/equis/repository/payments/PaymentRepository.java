package com.equestrian.equis.repository.payments;

import com.equestrian.equis.entity.payments.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    // A rider's payment history, voided ones included.
    List<Payment> findByRiderIdOrderByStartMonthAscRecordedAtAsc(UUID riderId);

    // The same history as W3 shows it: newest first.
    List<Payment> findByRiderIdOrderByRecordedAtDesc(UUID riderId);

    // The packages that count on date for these riders: not voided, started, not expired.
    @Query("""
            select p from Payment p
            where p.rider.id in :riderIds
              and p.voidedAt is null
              and p.startMonth <= :date
              and p.validUntil >= :date
            """)
    List<Payment> findValidOn(@Param("riderIds") Collection<UUID> riderIds, @Param("date") LocalDate date);
}
