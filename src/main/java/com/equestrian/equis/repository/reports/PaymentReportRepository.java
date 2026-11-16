package com.equestrian.equis.repository.reports;

import com.equestrian.equis.entity.reports.PaymentReportRow;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

public interface PaymentReportRepository extends Repository<PaymentReportRow, UUID> {
    // The current club's payments recorded in [from, to), voided ones included, in the order they were recorded.
    // Read inside a transaction, and close the stream.
    @Query("from PaymentReportRow where recordedAt >= :from and recordedAt < :to order by recordedAt, id")
    Stream<PaymentReportRow> streamRecorded(@Param("from") Instant from, @Param("to") Instant to);
}
