package com.icpizza.backend.management.mapper;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public final class Titles {
    private Titles() {
    }

    public static String monthPrefix(YearMonth ym) {
        String mon = ym.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
                .toLowerCase(java.util.Locale.ROOT);
        String yy = String.format("%02d", ym.getYear() % 100);
        return mon + "-" + yy;
    }

    public static YearMonth parseYearMonthPrefix(String title) {
        if (title == null || title.length() < 6) {
            throw new IllegalArgumentException("Invalid report title, expected 'MMM-yy...' prefix, got: " + title);
        }
        String prefix = title.substring(0, 6);
        DateTimeFormatter F = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("MMM-yy")
                .toFormatter(Locale.ENGLISH);
        return YearMonth.parse(prefix, F);
    }
}
