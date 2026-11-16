package com.equestrian.equis.repository.reports;

import com.equestrian.equis.entity.reports.ExpiredPackageReportRow;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

public interface ExpiredPackageReportRepository extends Repository<ExpiredPackageReportRow, UUID> {
    // The current club's packages with unused sessions whose last valid day is in [from, to] and before today, in
    // the order they expired, then by rider. Read inside a transaction, and close the stream.
    @Query("""
            from ExpiredPackageReportRow
            where validUntil >= :from and validUntil <= :to and validUntil < :today
            order by validUntil, riderName, id""")
    Stream<ExpiredPackageReportRow> streamExpired(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                                  @Param("today") LocalDate today);
}
