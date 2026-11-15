package com.equestrian.equis.service.reports;

import com.equestrian.equis.service.reports.AllTypesReport.Line;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// E18 · Excel writer: cases 1-13 and 17, read back with POI. AllTypesReport is the test-only "all-types" report.
class XlsxReportWriterTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private static final Instant GENERATED = ZonedDateTime.of(2026, 11, 14, 17, 46, 0, 0, CAIRO).toInstant();
    private static final ReportParams NOVEMBER = new ReportParams("Riding School", LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 14), CAIRO, GENERATED, Map.of());
    // Excel rows are 1-based, POI's 0-based: the header is Excel row 6, POI row 5.
    private static final int HEADER = 5;
    private static final int FIRST = 6;

    private final AllTypesReport withTotal = new AllTypesReport(true);
    private final AllTypesReport withoutTotal = new AllTypesReport(false);

    // 1
    @Test
    void theTitleBlockSaysWhichClubReportAndPeriod() throws Exception {
        try (XSSFWorkbook book = write(withTotal, Stream.empty())) {
            XSSFSheet sheet = book.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("All types");
            assertThat(text(sheet, 0)).isEqualTo("Riding School");
            assertThat(text(sheet, 1)).isEqualTo("All types");
            assertThat(text(sheet, 2)).isEqualTo("1 Nov 2026 – 30 Nov 2026");
            assertThat(text(sheet, 3)).isEqualTo("Generated 14 Nov 2026 17:46 (Cairo)");
            assertThat((Object) sheet.getRow(4)).isNull();
            assertThat(texts(sheet.getRow(HEADER))).containsExactly("Name", "Day", "At", "Amount", "Count");

            assertThat(font(book, sheet.getRow(0).getCell(0)).getBold()).isTrue();
            assertThat(font(book, sheet.getRow(0).getCell(0)).getFontHeightInPoints()).isEqualTo((short) 14);
            assertThat(font(book, sheet.getRow(1).getCell(0)).getFontHeightInPoints()).isEqualTo((short) 12);
        }
    }

    @Test
    void aSnapshotSaysAsOf() {
        ReportParams params = new ReportParams("Riding School", null, null, LocalDate.of(2026, 11, 4), CAIRO,
                GENERATED, Map.of());
        ReportDefinition<Line> snapshot = new AllTypesReport(false) {
            @Override
            public boolean dateRange() {
                return false;
            }
        };

        assertThat(XlsxReportWriter.period(snapshot, params)).isEqualTo("As of 4 Nov 2026");
    }

    // 2
    @Test
    void theHeaderIsBoldGreyFrozenAndFiltersOnlyTheDataRows() throws Exception {
        try (XSSFWorkbook book = write(withTotal, Stream.of(
                Line.of("A", new BigDecimal("1.00"), 1, false),
                Line.of("B", new BigDecimal("2.00"), 2, false)))) {
            XSSFSheet sheet = book.getSheetAt(0);
            Cell header = sheet.getRow(HEADER).getCell(0);
            assertThat(font(book, header).getBold()).isTrue();
            assertThat(header.getCellStyle().getFillPattern()).isEqualTo(FillPatternType.SOLID_FOREGROUND);
            assertThat(header.getCellStyle().getFillForegroundColor()).isEqualTo(IndexedColors.GREY_25_PERCENT.getIndex());

            PaneInformation pane = sheet.getPaneInformation();
            assertThat(pane.isFreezePane()).isTrue();
            assertThat(pane.getHorizontalSplitPosition()).isEqualTo((short) 6);
            assertThat(pane.getVerticalSplitPosition()).isZero();

            // Header (row 6) to the last data row (row 8); the total row (row 9) stays outside.
            assertThat(sheet.getCTWorksheet().getAutoFilter().getRef()).isEqualTo("A6:E8");
            assertThat(text(sheet, FIRST + 2)).isEqualTo(AllTypesReport.TOTAL_LABEL);
        }
    }

    // 3, 4, 5, 6
    @Test
    void everyValueIsWrittenAsItsType() throws Exception {
        try (XSSFWorkbook book = write(withTotal, Stream.of(new Line("Saddle", LocalDate.of(2026, 11, 2),
                Instant.parse("2026-11-02T08:30:00Z"), new BigDecimal("1500.00"), 8, false)))) {
            Row row = book.getSheetAt(0).getRow(FIRST);
            DataFormatter shown = new DataFormatter(java.util.Locale.ENGLISH);

            Cell day = row.getCell(1);
            assertThat(day.getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(day.getCellStyle().getDataFormatString()).isEqualTo("dd mmm yyyy");
            assertThat(day.getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 2));

            Cell at = row.getCell(2);
            assertThat(at.getCellStyle().getDataFormatString()).isEqualTo("dd mmm yyyy hh:mm");
            assertThat(at.getLocalDateTimeCellValue()).isEqualTo(LocalDateTime.of(2026, 11, 2, 10, 30));
            assertThat(shown.formatCellValue(at)).isEqualTo("02 Nov 2026 10:30");

            Cell amount = row.getCell(3);
            assertThat(amount.getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(amount.getNumericCellValue()).isEqualTo(1500.0);
            assertThat(amount.getCellStyle().getDataFormatString()).isEqualTo("#,##0.00 \"EGP\"");
            assertThat(shown.formatCellValue(amount)).isEqualTo("1,500.00 EGP");

            Cell count = row.getCell(4);
            assertThat(count.getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(count.getNumericCellValue()).isEqualTo(8.0);
            assertThat(count.getCellStyle().getDataFormatString()).isEqualTo("0");
        }
    }

    // 7
    @Test
    void aMissingValueIsAnEmptyCell() throws Exception {
        try (XSSFWorkbook book = write(withoutTotal, Stream.of(new Line(null, null, null, null, null, false)))) {
            Row row = book.getSheetAt(0).getRow(FIRST);
            for (int c = 0; c < 5; c++) {
                Cell cell = row == null ? null : row.getCell(c);
                assertThat(cell == null || cell.getCellType() == CellType.BLANK).as("column %d", c).isTrue();
            }
        }
    }

    // 8
    @Test
    void aValueOfTheWrongJavaTypeIsAProgrammingError() {
        Line wrong = new Line("Saddle", "2026-11-02", null, null, null, false);

        assertThatThrownBy(() -> write(withoutTotal, Stream.of(wrong)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("\"Day\"")
                .hasMessageContaining("LocalDate");
    }

    // 9
    @Test
    void theTotalRowAddsOnlyTheRowsTheReportCounts() throws Exception {
        try (XSSFWorkbook book = write(withTotal, Stream.of(
                Line.of("First", new BigDecimal("3000.00"), 1, false),
                Line.of("Second", new BigDecimal("6000.00"), 2, false),
                Line.of("Voided", new BigDecimal("3000.00"), 4, true)))) {
            XSSFSheet sheet = book.getSheetAt(0);
            Row total = sheet.getRow(FIRST + 3);
            assertThat(sheet.getLastRowNum()).isEqualTo(FIRST + 3);
            assertThat(total.getCell(0).getStringCellValue()).isEqualTo("Total (valid)");
            assertThat(total.getCell(3).getNumericCellValue()).isEqualTo(9000.0);
            assertThat(total.getCell(3).getCellStyle().getDataFormatString()).isEqualTo("#,##0.00 \"EGP\"");
            assertThat(total.getCell(4).getNumericCellValue()).isEqualTo(3.0);
            assertThat(font(book, total.getCell(0)).getBold()).isTrue();
            assertThat(font(book, total.getCell(3)).getBold()).isTrue();
            assertThat(total.getCell(3).getCellType()).isEqualTo(CellType.NUMERIC);
        }
    }

    // 10
    @Test
    void noRowsWithATotalGivesTheTitleHeaderAndZeroTotal() throws Exception {
        try (XSSFWorkbook book = write(withTotal, Stream.empty())) {
            XSSFSheet sheet = book.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(FIRST);
            Row total = sheet.getRow(FIRST);
            assertThat(total.getCell(0).getStringCellValue()).isEqualTo("Total (valid)");
            assertThat(total.getCell(3).getNumericCellValue()).isZero();
            assertThat(total.getCell(4).getNumericCellValue()).isZero();
            assertThat(sheet.getCTWorksheet().getAutoFilter().getRef()).isEqualTo("A6:E6");
        }
    }

    // 11
    @Test
    void noRowsWithoutATotalGivesTheTitleAndHeaderOnly() throws Exception {
        try (XSSFWorkbook book = write(withoutTotal, Stream.empty())) {
            XSSFSheet sheet = book.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(HEADER);
            assertThat(texts(sheet.getRow(HEADER))).containsExactly("Name", "Day", "At", "Amount", "Count");
        }
    }

    // 12
    @Test
    void arabicTextReadsBackUnchanged() throws Exception {
        try (XSSFWorkbook book = write(withoutTotal, Stream.of(Line.of("محمد عادل", null, null, false)))) {
            assertThat(text(book.getSheetAt(0), FIRST)).isEqualTo("محمد عادل");
        }
    }

    // 13
    @Test
    void columnsAreAsWideAsTheirLongestValueWithinLimits() throws Exception {
        try (XSSFWorkbook book = write(withoutTotal, Stream.of(
                new Line("x".repeat(30), null, null, null, null, false),
                new Line("y".repeat(200), null, null, null, null, false)))) {
            assertThat(book.getSheetAt(0).getColumnWidth(0) / 256).isEqualTo(60);
        }
        try (XSSFWorkbook book = write(withoutTotal, Stream.of(new Line("x".repeat(30), null, null, null, 7, false)))) {
            XSSFSheet sheet = book.getSheetAt(0);
            assertThat(sheet.getColumnWidth(0) / 256).isEqualTo(32);
            // "Count" and "7" are short.
            assertThat(sheet.getColumnWidth(4) / 256).isEqualTo(10);
        }
        assertThat(XlsxReportWriter.width(0)).isEqualTo(10);
        assertThat(XlsxReportWriter.width(30)).isEqualTo(32);
        assertThat(XlsxReportWriter.width(200)).isEqualTo(60);
    }

    // 17
    @Test
    void fiftyThousandRowsStreamAndLeaveNoTemporaryFiles() throws Exception {
        Set<String> before = sxssfTempFiles();
        Stream<Line> rows = IntStream.range(0, 50_000).mapToObj(i -> new Line("Rider " + i,
                LocalDate.of(2026, 11, 1).plusDays(i % 30), Instant.parse("2026-11-02T08:30:00Z"),
                new BigDecimal("100.00"), 1, i % 10 == 0));

        try (XSSFWorkbook book = write(withTotal, rows)) {
            XSSFSheet sheet = book.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(FIRST + 50_000);
            Row total = sheet.getRow(FIRST + 50_000);
            assertThat(total.getCell(3).getNumericCellValue()).isEqualTo(45_000 * 100.0);
            assertThat(total.getCell(4).getNumericCellValue()).isEqualTo(45_000.0);
        }
        assertThat(sxssfTempFiles()).isSubsetOf(before);
    }

    // ---- helpers

    private static XSSFWorkbook write(ReportDefinition<Line> report, Stream<Line> rows) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new XlsxReportWriter().write(report, NOVEMBER, rows, out);
        return new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()));
    }

    private static String text(XSSFSheet sheet, int row) {
        return sheet.getRow(row).getCell(0).getStringCellValue();
    }

    private static List<String> texts(Row row) {
        List<String> texts = new ArrayList<>();
        row.forEach(cell -> texts.add(cell.getStringCellValue()));
        return texts;
    }

    private static org.apache.poi.ss.usermodel.Font font(XSSFWorkbook book, Cell cell) {
        return book.getFontAt(cell.getCellStyle().getFontIndex());
    }

    // SXSSF keeps the streamed rows in files named poi-sxssf-sheet*.xml(.gz) under java.io.tmpdir/poifiles.
    private static Set<String> sxssfTempFiles() {
        File[] files = new File(System.getProperty("java.io.tmpdir"), "poifiles")
                .listFiles((dir, name) -> name.startsWith("poi-sxssf-sheet"));
        return files == null ? Set.of() : Stream.of(files).map(File::getName).collect(java.util.stream.Collectors.toSet());
    }
}
