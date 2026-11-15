package com.equestrian.equis.service.reports;

import java.util.function.Function;

// One column of a report: its header, how it is shown, and how to read its value from a row.
public record ReportColumn<R>(String header, ColumnType type, Function<R, ?> value) {

    public static <R> ReportColumn<R> text(String header, Function<R, ?> value) {
        return new ReportColumn<>(header, ColumnType.TEXT, value);
    }

    public static <R> ReportColumn<R> date(String header, Function<R, ?> value) {
        return new ReportColumn<>(header, ColumnType.DATE, value);
    }

    public static <R> ReportColumn<R> dateTime(String header, Function<R, ?> value) {
        return new ReportColumn<>(header, ColumnType.DATETIME, value);
    }

    public static <R> ReportColumn<R> money(String header, Function<R, ?> value) {
        return new ReportColumn<>(header, ColumnType.MONEY_EGP, value);
    }

    public static <R> ReportColumn<R> integer(String header, Function<R, ?> value) {
        return new ReportColumn<>(header, ColumnType.INTEGER, value);
    }

    public Object valueOf(R row) {
        return value.apply(row);
    }
}
