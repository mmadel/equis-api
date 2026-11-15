package com.equestrian.equis.controller.reports;

import com.equestrian.equis.service.reports.ReportExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

// E18 · the admin's report exports. SecurityConfig limits /admin/** to the admin; it is a GET, so a read-only club
// can still export. from/to (YYYY-MM-DD) for range reports; other parameters are the report's own filters.
@RestController
public class ReportController {
    private final ReportExportService service;

    public ReportController(ReportExportService service) {
        this.service = service;
    }

    @GetMapping("/admin/reports/{name}/export")
    void export(@PathVariable String name, @RequestParam Map<String, String> query, HttpServletResponse response)
            throws IOException {
        service.export(name, query, response);
    }
}
