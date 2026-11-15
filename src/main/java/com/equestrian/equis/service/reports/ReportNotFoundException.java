package com.equestrian.equis.service.reports;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import org.springframework.http.HttpStatus;

import java.util.Map;

// 404 report_not_found: no report has this name. name is the name asked for.
public class ReportNotFoundException extends ApiException {
    public ReportNotFoundException(String name) {
        super(HttpStatus.NOT_FOUND, ErrorCodes.REPORT_NOT_FOUND, "There is no report with this name.",
                Map.of("name", name));
    }
}
