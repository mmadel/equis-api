package com.equestrian.equis.service.reports;

import com.equestrian.equis.common.error.ErrorCodes;
import org.junit.jupiter.api.Test;

import java.util.List;
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
