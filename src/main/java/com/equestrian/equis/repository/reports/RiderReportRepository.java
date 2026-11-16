package com.equestrian.equis.repository.reports;

import com.equestrian.equis.entity.reports.RiderReportRow;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.UUID;
import java.util.stream.Stream;

public interface RiderReportRepository extends Repository<RiderReportRow, UUID> {
    // Every rider of the current club, by name. Read inside a transaction, and close the stream.
    @Query("from RiderReportRow order by riderName, id")
    Stream<RiderReportRow> streamAll();
}
