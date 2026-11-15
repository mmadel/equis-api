package com.equestrian.equis.service.reports;

// How a report column's values are shown in the file. The plain writer writes numbers as numbers and the rest as
// text; story 45 gives each type its own cell format.
public enum ColumnType {
    TEXT,
    DATE,
    DATETIME,
    MONEY_EGP,
    INTEGER
}
