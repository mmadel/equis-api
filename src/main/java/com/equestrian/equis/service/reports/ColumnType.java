package com.equestrian.equis.service.reports;

// How a report column's values are shown in the file: each type has its own cell format (ExcelStyles) and Java
// value type (TEXT any, DATE LocalDate, DATETIME Instant, MONEY_EGP BigDecimal, INTEGER Integer or Long).
public enum ColumnType {
    TEXT,
    DATE,
    DATETIME,
    MONEY_EGP,
    INTEGER
}
