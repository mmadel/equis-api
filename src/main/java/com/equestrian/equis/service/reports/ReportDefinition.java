package com.equestrian.equis.service.reports;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

// One report: a @Component the ReportRegistry finds at startup. A new report needs only a view, an @Immutable
// entity on it and one of these.
public interface ReportDefinition<R> {
    // URL segment and file name prefix, lowercase-kebab, e.g. "trainers".
    String name();

    // Shown in the file (the sheet name now, the title block in story 45).
    String title();

    // true: the report covers from..to, which are required. false: a snapshot as of today.
    boolean dateRange();

    List<ReportColumn<R>> columns();

    // The rows, only the current club's. Called inside a read-only transaction; the caller closes the stream.
    Stream<R> rows(ReportParams params);

    default Optional<ReportTotal<R>> total() {
        return Optional.empty();
    }
}
