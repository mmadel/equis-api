package com.equestrian.equis.service.reports;

import com.equestrian.equis.common.error.ErrorCodes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// E18 · export engine, case 13 and the lookup by name.
class ReportRegistryTest {

    @Test
    void twoReportsWithTheSameNameFailAtConstruction() {
        assertThatThrownBy(() -> new ReportRegistry(List.of(new Named("trainers"), new Named("trainers"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Two reports are named \"trainers\"");
    }

    @Test
    void findsAReportByName() {
        Named trainers = new Named("trainers");
        ReportRegistry registry = new ReportRegistry(List.of(trainers, new Named("payments")));

        assertThat(registry.get("trainers")).isSameAs(trainers);
    }

    @Test
    void anUnknownNameIsReportNotFound() {
        ReportRegistry registry = new ReportRegistry(List.of(new Named("trainers")));

        assertThatThrownBy(() -> registry.get("unknown"))
                .isInstanceOfSatisfying(ReportNotFoundException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(404);
                    assertThat(e.code()).isEqualTo(ErrorCodes.REPORT_NOT_FOUND);
                    assertThat(e.properties()).containsEntry("name", "unknown");
                });
    }

    // E18 · Excel writer, case 16: a total over a text column or an unknown header stops the app at startup.
    @Test
    void aTotalOverATextColumnFailsAtConstruction() {
        assertThatThrownBy(() -> new ReportRegistry(List.of(new Totalled("Name"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("\"totalled\"")
                .hasMessageContaining("\"Name\"")
                .hasMessageContaining("TEXT");
    }

    @Test
    void aTotalOverAnUnknownHeaderFailsAtConstruction() {
        assertThatThrownBy(() -> new ReportRegistry(List.of(new Totalled("Amount", "Price"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("\"Price\"")
                .hasMessageContaining("no such column");
    }

    @Test
    void aTotalOverMoneyAndIntegerColumnsIsAccepted() {
        ReportRegistry registry = new ReportRegistry(List.of(new Totalled("Amount", "Count"), new AllTypesReport(true)));

        assertThat(registry.get("totalled")).isNotNull();
    }

    private record Totalled(List<String> sumHeaders) implements ReportDefinition<String> {
        Totalled(String... sumHeaders) {
            this(List.of(sumHeaders));
        }

        @Override
        public String name() {
            return "totalled";
        }

        @Override
        public String title() {
            return "Totalled";
        }

        @Override
        public boolean dateRange() {
            return false;
        }

        @Override
        public List<ReportColumn<String>> columns() {
            return List.of(ReportColumn.text("Label", s -> s), ReportColumn.text("Name", s -> s),
                    ReportColumn.money("Amount", s -> null), ReportColumn.integer("Count", s -> null));
        }

        @Override
        public Stream<String> rows(ReportParams params) {
            return Stream.empty();
        }

        @Override
        public Optional<ReportTotal<String>> total() {
            return Optional.of(new ReportTotal<>("Total", s -> true, sumHeaders));
        }
    }

    private record Named(String name) implements ReportDefinition<String> {
        @Override
        public String title() {
            return name;
        }

        @Override
        public boolean dateRange() {
            return false;
        }

        @Override
        public List<ReportColumn<String>> columns() {
            return List.of();
        }

        @Override
        public Stream<String> rows(ReportParams params) {
            return Stream.empty();
        }
    }
}
