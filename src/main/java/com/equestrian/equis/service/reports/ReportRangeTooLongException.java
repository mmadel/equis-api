package com.equestrian.equis.service.reports;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import org.springframework.http.HttpStatus;

import java.util.Map;

// 400 report_range_too_long: the dates cover more than maxMonths months.
public class ReportRangeTooLongException extends ApiException {
    public ReportRangeTooLongException(int maxMonths) {
        super(HttpStatus.BAD_REQUEST, ErrorCodes.REPORT_RANGE_TOO_LONG,
                "A report can cover at most " + maxMonths + " months.", Map.of("maxMonths", maxMonths));
    }
}
