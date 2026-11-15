package com.equestrian.equis.service.reports;

import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Stream;

// Writes a report's rows as a file. The caller opens and closes the rows and the output.
public interface ReportWriter {
    <R> void write(ReportDefinition<R> report, ReportParams params, Stream<R> rows, OutputStream out)
            throws IOException;
}
