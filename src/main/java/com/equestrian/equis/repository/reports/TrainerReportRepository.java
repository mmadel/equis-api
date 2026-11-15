package com.equestrian.equis.repository.reports;

import com.equestrian.equis.entity.reports.TrainerReportRow;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.UUID;
import java.util.stream.Stream;

public interface TrainerReportRepository extends Repository<TrainerReportRow, UUID> {
    // The current club's trainers by name. Read inside a transaction, and close the stream.
    @Query("from TrainerReportRow order by name, id")
    Stream<TrainerReportRow> streamAll();
}
