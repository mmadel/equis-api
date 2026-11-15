package com.equestrian.equis.service.reports;

import java.util.List;
import java.util.function.Predicate;

// The total row of a report, written right after the last data row. label goes in the first column; each column
// named in sumHeaders (a MONEY_EGP or INTEGER column, checked by ReportRegistry at startup) is summed over the rows
// where include is true, e.g. valid payments only. The writer adds up while the rows stream, so no formulas.
public record ReportTotal<R>(String label, Predicate<R> include, List<String> sumHeaders) {

    public ReportTotal {
        sumHeaders = List.copyOf(sumHeaders);
    }

    // Every row counts.
    public static <R> ReportTotal<R> of(String label, String... sumHeaders) {
        return new ReportTotal<>(label, row -> true, List.of(sumHeaders));
    }
}
