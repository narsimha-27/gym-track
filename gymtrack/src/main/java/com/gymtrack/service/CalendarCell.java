package com.gymtrack.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** One square of the month calendar. day = 0 means an empty filler square. */
public record CalendarCell(int day, boolean attended, boolean today, boolean future) {

    public static List<CalendarCell> build(YearMonth month, Set<Integer> attendedDays, LocalDate today) {
        List<CalendarCell> cells = new ArrayList<>();

        int blanks = month.atDay(1).getDayOfWeek().getValue() - 1;   // Monday = 1
        for (int i = 0; i < blanks; i++) {
            cells.add(new CalendarCell(0, false, false, false));
        }
        for (int d = 1; d <= month.lengthOfMonth(); d++) {
            LocalDate date = month.atDay(d);
            cells.add(new CalendarCell(d, attendedDays.contains(d), date.equals(today), date.isAfter(today)));
        }
        return cells;
    }
}