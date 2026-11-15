package com.equestrian.equis.service.reports;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Builds a report's ReportParams from the query and checks the dates.
// A range report needs from and to (YYYY-MM-DD, whole days in the club's zone, both included), in order, and to
// must be before from + 12 months: 1 Jan to 31 Dec is allowed, 1 Jan to 1 Jan of the next year is not.
// A snapshot report ignores from and to. Every other parameter is passed to the report as a filter.
@Component
public class ReportParamsFactory {
    public static final int MAX_MONTHS = 12;
    static final String FROM = "from";
    static final String TO = "to";

    private final Clock clock;
    private final ZoneId zone;

    public ReportParamsFactory(Clock clock, @Value("${app.time-zone}") ZoneId zone) {
        this.clock = clock;
        this.zone = zone;
    }

    public ReportParams build(ReportDefinition<?> report, Map<String, String> query) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, zone);
        Map<String, String> filters = new HashMap<>(query);
        filters.remove(FROM);
        filters.remove(TO);
        if (!report.dateRange()) {
            return new ReportParams(null, null, today, zone, now, filters);
        }

        List<ApiFieldError> errors = new ArrayList<>();
        LocalDate from = date(query, FROM, errors);
        LocalDate to = date(query, TO, errors);
        if (from != null && to != null && from.isAfter(to)) {
            errors.add(new ApiFieldError(TO, ErrorCodes.INVALID_RANGE));
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Some fields are not valid.",
                    Map.of("fieldErrors", errors));
        }
        if (!to.isBefore(from.plusMonths(MAX_MONTHS))) {
            throw new ReportRangeTooLongException(MAX_MONTHS);
        }
        return new ReportParams(from, to, today, zone, now, filters);
    }

    private static LocalDate date(Map<String, String> query, String field, List<ApiFieldError> errors) {
        String value = query.get(field);
        if (value == null || value.isBlank()) {
            errors.add(new ApiFieldError(field, "must not be null"));
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            errors.add(new ApiFieldError(field, ErrorCodes.INVALID_DATE));
            return null;
        }
    }
}
