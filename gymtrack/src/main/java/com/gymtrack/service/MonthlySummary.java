package com.gymtrack.service;

import java.time.Duration;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/** Attendance summary of one member for one month. */
public record MonthlySummary(YearMonth month, int daysPresent, Duration totalTime) {

    public String label() {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();
    }

    public String totalText() {
        long mins = totalTime.toMinutes();
        return (mins / 60) + "h " + (mins % 60) + "m";
    }
}
