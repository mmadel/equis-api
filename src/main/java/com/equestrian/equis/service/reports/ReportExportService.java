package com.equestrian.equis.service.reports;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.stream.Stream;

// GET /admin/reports/{name}/export: finds the report, checks the query, and writes the file into the response.
// Everything runs in the request thread inside one read-only transaction, so the club of the request and the open
// database cursor stay valid while the rows stream out. Every check runs before the first byte is written, so an
// error still answers as ProblemDetail JSON.
@Service
public class ReportExportService {
    public static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReportRegistry registry;
    private final ReportParamsFactory paramsFactory;
    private final ReportWriter writer;

    public ReportExportService(ReportRegistry registry, ReportParamsFactory paramsFactory, ReportWriter writer) {
        this.registry = registry;
        this.paramsFactory = paramsFactory;
        this.writer = writer;
    }

    @Transactional(readOnly = true)
    public void export(String name, Map<String, String> query, HttpServletResponse response) throws IOException {
        ReportDefinition<?> report = registry.get(name);
        ReportParams params = paramsFactory.build(report, query);
        write(report, params, response);
    }

    // {name}_{from}_{to}.xlsx for a range report, {name}_{asOf}.xlsx for a snapshot.
    static String fileName(ReportDefinition<?> report, ReportParams params) {
        return report.dateRange()
                ? report.name() + "_" + params.from() + "_" + params.to() + ".xlsx"
                : report.name() + "_" + params.asOf() + ".xlsx";
    }

    private <R> void write(ReportDefinition<R> report, ReportParams params, HttpServletResponse response)
            throws IOException {
        response.setContentType(XLSX);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(fileName(report, params)).build().toString());
        try (Stream<R> rows = report.rows(params)) {
            OutputStream out = response.getOutputStream();
            writer.write(report, params, rows, out);
            out.flush();
        }
    }
}
