package com.equestrian.equis.service.reports;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// Every ReportDefinition bean, by name. Two reports with the same name, or a total over a column that doesn't exist
// or isn't a number, stop the app at startup.
@Component
public class ReportRegistry {
    private static final Set<ColumnType> SUMMABLE = EnumSet.of(ColumnType.MONEY_EGP, ColumnType.INTEGER);

    private final Map<String, ReportDefinition<?>> reports;

    public ReportRegistry(List<ReportDefinition<?>> definitions) {
        definitions.forEach(ReportRegistry::checkTotal);
        this.reports = definitions.stream().collect(Collectors.toUnmodifiableMap(ReportDefinition::name,
                Function.identity(), (first, second) -> {
                    throw new IllegalStateException("Two reports are named \"" + first.name() + "\": "
                            + first.getClass().getName() + " and " + second.getClass().getName());
                }));
    }

    public ReportDefinition<?> get(String name) {
        ReportDefinition<?> report = reports.get(name);
        if (report == null) {
            throw new ReportNotFoundException(name);
        }
        return report;
    }

    // Each summed header names a MONEY_EGP or INTEGER column, and not the first column, which holds the label.
    private static <R> void checkTotal(ReportDefinition<R> report) {
        report.total().ifPresent(total -> {
            List<ReportColumn<R>> columns = report.columns();
            for (String header : total.sumHeaders()) {
                int index = indexOf(columns, header);
                if (index < 0) {
                    throw badTotal(report, header, "there is no such column");
                }
                if (!SUMMABLE.contains(columns.get(index).type())) {
                    throw badTotal(report, header, "it is " + columns.get(index).type() + ", not MONEY_EGP or INTEGER");
                }
                if (index == 0) {
                    throw badTotal(report, header, "the first column holds the total's label");
                }
            }
        });
    }

    private static int indexOf(List<? extends ReportColumn<?>> columns, String header) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).header().equals(header)) {
                return i;
            }
        }
        return -1;
    }

    private static IllegalStateException badTotal(ReportDefinition<?> report, String header, String why) {
        return new IllegalStateException("Report \"" + report.name() + "\" totals column \"" + header + "\", but "
                + why);
    }
}
