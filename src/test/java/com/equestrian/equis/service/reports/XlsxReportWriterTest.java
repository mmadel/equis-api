package com.equestrian.equis.service.reports;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// E18 · export engine: the plain writer's header row and cells (decision 2).
class XlsxReportWriterTest {
    private static final ReportParams PARAMS = new ReportParams(null, null, LocalDate.of(2026, 11, 14),
            ZoneId.of("Africa/Cairo"), Instant.parse("2026-11-14T10:00:00Z"), Map.of());

    record Item(String name, Integer count, LocalDate day) {
    }

    private final ReportDefinition<Item> report = new ReportDefinition<>() {
        @Override
        public String name() {
            return "items";
        }

        @Override
        public String title() {
            return "Items: [all]";
        }

        @Override
        public boolean dateRange() {
            return false;
        }

        @Override
        public List<ReportColumn<Item>> columns() {
            return List.of(ReportColumn.text("Name", Item::name), ReportColumn.integer("Count", Item::count),
                    ReportColumn.date("Day", Item::day));
        }

        @Override
        public Stream<Item> rows(ReportParams params) {
            throw new UnsupportedOperationException();
        }
    };

    @Test
    void writesAHeaderAndOneRowPerRecord() throws Exception {
        try (XSSFWorkbook book = write(Stream.of(
                new Item("Saddle", 3, LocalDate.of(2026, 11, 1)),
                new Item("Bridle", null, null)))) {
            Sheet sheet = book.getSheetAt(0);
            assertThat(book.getNumberOfSheets()).isEqualTo(1);
            // ':' and '[' ']' aren't allowed in a sheet name; POI swaps them out.
            assertThat(sheet.getSheetName()).doesNotContain(":", "[", "]").startsWith("Items");
            assertThat(texts(sheet.getRow(0))).containsExactly("Name", "Count", "Day");

            Row first = sheet.getRow(1);
            assertThat(first.getCell(0).getStringCellValue()).isEqualTo("Saddle");
            assertThat(first.getCell(1).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(first.getCell(1).getNumericCellValue()).isEqualTo(3.0);
            assertThat(first.getCell(2).getStringCellValue()).isEqualTo("2026-11-01");

            Row second = sheet.getRow(2);
            assertThat(second.getCell(1).getCellType()).isEqualTo(CellType.BLANK);
            assertThat(second.getCell(2).getCellType()).isEqualTo(CellType.BLANK);
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
        }
    }

    @Test
    void noRecordsGivesTheHeaderOnly() throws Exception {
        try (XSSFWorkbook book = write(Stream.empty())) {
            Sheet sheet = book.getSheetAt(0);
            assertThat(texts(sheet.getRow(0))).containsExactly("Name", "Count", "Day");
            assertThat(sheet.getLastRowNum()).isZero();
        }
    }

    private XSSFWorkbook write(Stream<Item> rows) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new XlsxReportWriter().write(report, PARAMS, rows, out);
        return new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()));
    }

    private static List<String> texts(Row row) {
        List<String> texts = new java.util.ArrayList<>();
        row.forEach(cell -> texts.add(cell.getStringCellValue()));
        return texts;
    }
}
