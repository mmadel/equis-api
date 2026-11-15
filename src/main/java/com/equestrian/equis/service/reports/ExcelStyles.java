package com.equestrian.equis.service.reports;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.EnumMap;
import java.util.Map;

// The styles of one report file, created once per workbook (a workbook holds at most 64,000 styles, so never one
// per cell): the title block, the header, one per column type and the total row.
final class ExcelStyles {
    static final String DATE_FORMAT = "dd mmm yyyy";
    static final String DATETIME_FORMAT = "dd mmm yyyy hh:mm";
    static final String MONEY_FORMAT = "#,##0.00 \"EGP\"";
    static final String INTEGER_FORMAT = "0";
    static final String TEXT_FORMAT = "@";

    private final CellStyle title;
    private final CellStyle subtitle;
    private final CellStyle meta;
    private final CellStyle header;
    private final CellStyle totalLabel;
    private final Map<ColumnType, CellStyle> cells = new EnumMap<>(ColumnType.class);
    private final Map<ColumnType, CellStyle> totals = new EnumMap<>(ColumnType.class);

    ExcelStyles(Workbook workbook) {
        Font bold = workbook.createFont();
        bold.setBold(true);

        title = workbook.createCellStyle();
        title.setFont(boldFont(workbook, 14));
        subtitle = workbook.createCellStyle();
        subtitle.setFont(boldFont(workbook, 12));
        meta = workbook.createCellStyle();

        header = workbook.createCellStyle();
        header.setFont(bold);
        header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setBorderBottom(BorderStyle.THIN);

        totalLabel = workbook.createCellStyle();
        totalLabel.setFont(bold);
        totalLabel.setBorderTop(BorderStyle.THIN);

        for (ColumnType type : ColumnType.values()) {
            short format = workbook.createDataFormat().getFormat(format(type));
            CellStyle cell = workbook.createCellStyle();
            cell.setDataFormat(format);
            cells.put(type, cell);

            CellStyle total = workbook.createCellStyle();
            total.setDataFormat(format);
            total.setFont(bold);
            total.setBorderTop(BorderStyle.THIN);
            totals.put(type, total);
        }
    }

    static String format(ColumnType type) {
        return switch (type) {
            case TEXT -> TEXT_FORMAT;
            case DATE -> DATE_FORMAT;
            case DATETIME -> DATETIME_FORMAT;
            case MONEY_EGP -> MONEY_FORMAT;
            case INTEGER -> INTEGER_FORMAT;
        };
    }

    CellStyle title() {
        return title;
    }

    CellStyle subtitle() {
        return subtitle;
    }

    CellStyle meta() {
        return meta;
    }

    CellStyle header() {
        return header;
    }

    CellStyle cell(ColumnType type) {
        return cells.get(type);
    }

    // A cell of the total row: the label, a blank under a column that isn't summed, or a sum.
    CellStyle totalLabel() {
        return totalLabel;
    }

    CellStyle total(ColumnType type) {
        return totals.get(type);
    }

    private static Font boldFont(Workbook workbook, int points) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) points);
        return font;
    }
}
