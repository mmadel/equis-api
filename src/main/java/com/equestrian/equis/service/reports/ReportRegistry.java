package com.equestrian.equis.service.reports;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Every ReportDefinition bean, by name. Two reports with the same name stop the app at startup.
@Component
public class ReportRegistry {
    private final Map<String, ReportDefinition<?>> reports;

    public ReportRegistry(List<ReportDefinition<?>> definitions) {
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
}
