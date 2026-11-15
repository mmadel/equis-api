package com.equestrian.equis.service.reports;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

// The plain .xlsx writer: one sheet named after the report's title, a header row with the column names, then one
// row per record. Numbers are number cells, everything else is text, an empty value an empty cell. Streamed
// (SXSSFWorkbook keeps only a window of rows in memory), so a long report doesn't fill the heap.
// Story 45 adds the title block, styles, typed formats, total row and column widths.
@Component
public class XlsxReportWriter implements ReportWriter {
    private static final int ROWS_IN_MEMORY = 100;

    @Override
    public <R> void write(ReportDefinition<R> report, ReportParams params, Stream<R> rows, OutputStream out)
            throws IOException {
        SXSSFWorkbook workbook = new SXSSFWorkbook(ROWS_IN_MEMORY);
        try {
            Sheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(report.title()));
            List<ReportColumn<R>> columns = report.columns();
            Row header = sheet.createRow(0);
            for (int c = 0; c < columns.size(); c++) {
                header.createCell(c).setCellValue(columns.get(c).header());
            }
            int r = 1;
            for (Iterator<R> it = rows.iterator(); it.hasNext(); r++) {
                R record = it.next();
                Row row = sheet.createRow(r);
                for (int c = 0; c < columns.size(); c++) {
                    setValue(row.createCell(c), columns.get(c).valueOf(record));
                }
            }
            workbook.write(out);
        } finally {
            // Deletes the temporary files the streamed rows were kept in.
            workbook.dispose();
            workbook.close();
        }
    }

    private static void setValue(Cell cell, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
