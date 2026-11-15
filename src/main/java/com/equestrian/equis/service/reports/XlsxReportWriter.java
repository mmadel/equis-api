package com.equestrian.equis.service.reports;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

// The .xlsx writer. One sheet, named after the report's title:
//   row 1  club name (bold 14)                 row 2  report title (bold 12)
//   row 3  "1 Nov 2026 – 30 Nov 2026" or "As of 14 Nov 2026"
//   row 4  "Generated 14 Nov 2026 17:46 (Cairo)"   row 5  empty
//   row 6  header: bold, grey, bottom border, frozen, with an auto-filter down to the last data row
//   row 7+ one row per record, each value a typed cell (ExcelStyles); null is an empty cell
//   then   the total row, when the report has one, summed here while the rows stream
// Column widths follow the longest value of each column (header and total included), clamped to 10..60 characters.
// Streamed: SXSSFWorkbook keeps only a window of rows in memory, the rest in compressed temporary files.
@Component
public class XlsxReportWriter implements ReportWriter {
    static final int ROWS_IN_MEMORY = 100;
    static final int HEADER_ROW = 5;
    static final int MIN_WIDTH = 10;
    static final int MAX_WIDTH = 60;

    private static final DateTimeFormatter TITLE_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TITLE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale.ENGLISH);
    // How the cells read in Excel, for the column widths only.
    private static final DateTimeFormatter CELL_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter CELL_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH);

    @Override
    public <R> void write(ReportDefinition<R> report, ReportParams params, Stream<R> rows, OutputStream out)
            throws IOException {
        SXSSFWorkbook workbook = new SXSSFWorkbook(ROWS_IN_MEMORY);
        workbook.setCompressTempFiles(true);
        try {
            new SheetWriter<>(report, params, workbook).write(rows);
            workbook.write(out);
        } finally {
            // Deletes the temporary files the streamed rows were kept in.
            workbook.dispose();
            workbook.close();
        }
    }

    // "1 Nov 2026 – 30 Nov 2026" for a range report, "As of 14 Nov 2026" for a snapshot.
    static String period(ReportDefinition<?> report, ReportParams params) {
        return report.dateRange()
                ? TITLE_DATE.format(params.from()) + " – " + TITLE_DATE.format(params.to())
                : "As of " + TITLE_DATE.format(params.asOf());
    }

    // "Generated 14 Nov 2026 17:46 (Cairo)".
    static String generated(ReportParams params) {
        return "Generated " + TITLE_TIME.format(params.generatedAt().atZone(params.zone()))
                + " (" + city(params.zone()) + ")";
    }

    // Africa/Cairo -> Cairo.
    private static String city(ZoneId zone) {
        String id = zone.getId();
        return id.substring(id.lastIndexOf('/') + 1).replace('_', ' ');
    }

    // The state of one file being written: styles, widths and running totals.
    private static final class SheetWriter<R> {
        private final ReportDefinition<R> report;
        private final ReportParams params;
        private final List<ReportColumn<R>> columns;
        private final Optional<ReportTotal<R>> total;
        private final Sheet sheet;
        private final ExcelStyles styles;
        private final int[] longest;
        private final boolean[] summed;
        private final BigDecimal[] money;
        private final long[] integers;
        private final DecimalFormat moneyText = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

        SheetWriter(ReportDefinition<R> report, ReportParams params, SXSSFWorkbook workbook) {
            this.report = report;
            this.params = params;
            this.columns = report.columns();
            this.total = report.total();
            this.sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(report.title()));
            this.styles = new ExcelStyles(workbook);
            this.longest = new int[columns.size()];
            this.summed = new boolean[columns.size()];
            this.money = new BigDecimal[columns.size()];
            this.integers = new long[columns.size()];
            total.ifPresent(t -> {
                for (int c = 0; c < columns.size(); c++) {
                    summed[c] = t.sumHeaders().contains(columns.get(c).header());
                    money[c] = BigDecimal.ZERO;
                }
            });
        }

        void write(Stream<R> rows) {
            text(0, params.clubName(), styles.title());
            text(1, report.title(), styles.subtitle());
            text(2, period(report, params), styles.meta());
            text(3, generated(params), styles.meta());

            Row header = sheet.createRow(HEADER_ROW);
            for (int c = 0; c < columns.size(); c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(columns.get(c).header());
                cell.setCellStyle(styles.header());
                measure(c, columns.get(c).header());
            }

            int r = HEADER_ROW + 1;
            for (Iterator<R> it = rows.iterator(); it.hasNext(); r++) {
                R record = it.next();
                boolean counts = total.map(t -> t.include().test(record)).orElse(false);
                Row row = sheet.createRow(r);
                for (int c = 0; c < columns.size(); c++) {
                    ReportColumn<R> column = columns.get(c);
                    Object value = column.valueOf(record);
                    if (value == null) {
                        continue;
                    }
                    Cell cell = row.createCell(c);
                    cell.setCellStyle(styles.cell(column.type()));
                    measure(c, set(cell, column, value));
                    if (counts && summed[c]) {
                        add(c, column.type(), value);
                    }
                }
            }
            int lastDataRow = r - 1;
            total.ifPresent(t -> writeTotal(t, lastDataRow + 1));

            sheet.createFreezePane(0, HEADER_ROW + 1);
            if (!columns.isEmpty()) {
                sheet.setAutoFilter(new CellRangeAddress(HEADER_ROW, lastDataRow, 0, columns.size() - 1));
            }
            for (int c = 0; c < columns.size(); c++) {
                sheet.setColumnWidth(c, width(longest[c]) * 256);
            }
        }

        // Writes value as the column's type and returns how it reads in Excel.
        private String set(Cell cell, ReportColumn<R> column, Object value) {
            switch (column.type()) {
                case TEXT -> {
                    String text = value.toString();
                    cell.setCellValue(text);
                    return text;
                }
                case DATE -> {
                    LocalDate date = expect(column, value, LocalDate.class);
                    cell.setCellValue(date);
                    return CELL_DATE.format(date);
                }
                case DATETIME -> {
                    LocalDateTime local = LocalDateTime.ofInstant(expect(column, value, Instant.class), params.zone());
                    cell.setCellValue(local);
                    return CELL_TIME.format(local);
                }
                case MONEY_EGP -> {
                    BigDecimal amount = expect(column, value, BigDecimal.class);
                    cell.setCellValue(amount.doubleValue());
                    return moneyText(amount);
                }
                case INTEGER -> {
                    if (!(value instanceof Integer) && !(value instanceof Long)) {
                        throw wrongType(column, value, "Integer or Long");
                    }
                    long number = ((Number) value).longValue();
                    cell.setCellValue(number);
                    return Long.toString(number);
                }
                default -> throw new IllegalStateException("Unknown column type " + column.type());
            }
        }

        private void add(int c, ColumnType type, Object value) {
            if (type == ColumnType.MONEY_EGP) {
                money[c] = money[c].add((BigDecimal) value);
            } else {
                integers[c] += ((Number) value).longValue();
            }
        }

        private void writeTotal(ReportTotal<R> t, int index) {
            Row row = sheet.createRow(index);
            for (int c = 0; c < columns.size(); c++) {
                ColumnType type = columns.get(c).type();
                Cell cell = row.createCell(c);
                if (c == 0) {
                    cell.setCellValue(t.label());
                    cell.setCellStyle(styles.totalLabel());
                    measure(c, t.label());
                } else if (!summed[c]) {
                    cell.setCellStyle(styles.totalLabel());
                } else if (type == ColumnType.MONEY_EGP) {
                    cell.setCellValue(money[c].doubleValue());
                    cell.setCellStyle(styles.total(type));
                    measure(c, moneyText(money[c]));
                } else {
                    cell.setCellValue(integers[c]);
                    cell.setCellStyle(styles.total(type));
                    measure(c, Long.toString(integers[c]));
                }
            }
        }

        private void text(int index, String value, CellStyle style) {
            Cell cell = sheet.createRow(index).createCell(0);
            if (value != null) {
                cell.setCellValue(value);
            }
            cell.setCellStyle(style);
        }

        private void measure(int c, String text) {
            longest[c] = Math.max(longest[c], text.length());
        }

        private String moneyText(BigDecimal amount) {
            return moneyText.format(amount) + " EGP";
        }

        private <T> T expect(ReportColumn<R> column, Object value, Class<T> type) {
            if (!type.isInstance(value)) {
                throw wrongType(column, value, type.getSimpleName());
            }
            return type.cast(value);
        }

        private IllegalStateException wrongType(ReportColumn<R> column, Object value, String expected) {
            return new IllegalStateException("Report \"" + report.name() + "\" column \"" + column.header() + "\" is "
                    + column.type() + " and needs " + expected + ", got " + value.getClass().getName());
        }
    }

    // clamp(longest + 2, 10, 60) characters.
    static int width(int longest) {
        return Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, longest + 2));
    }
}
