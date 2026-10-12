package com.gymtrack.service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** The gym's opening hours. Change the four times below to change them everywhere in the app. */
public final class GymTimings {

    public static final LocalTime MORNING_OPEN = LocalTime.of(6, 0);
    public static final LocalTime MORNING_CLOSE = LocalTime.of(11, 0);
    public static final LocalTime EVENING_OPEN = LocalTime.of(16, 0);
    public static final LocalTime EVENING_CLOSE = LocalTime.of(21, 0);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private GymTimings() { }

    public static boolean isOpen(LocalTime t) {
        return within(t, MORNING_OPEN, MORNING_CLOSE) || within(t, EVENING_OPEN, EVENING_CLOSE);
    }

    private static boolean within(LocalTime t, LocalTime open, LocalTime close) {
        return !t.isBefore(open) && t.isBefore(close);
    }

    public static String morningText() {
        return FMT.format(MORNING_OPEN) + " - " + FMT.format(MORNING_CLOSE);
    }

    public static String eveningText() {
        return FMT.format(EVENING_OPEN) + " - " + FMT.format(EVENING_CLOSE);
    }

    public static String hoursText() {
        return "Morning " + morningText() + ", Evening " + eveningText();
    }

    public static String statusText(LocalTime t) {
        if (within(t, MORNING_OPEN, MORNING_CLOSE)) return "Open now, closes at " + FMT.format(MORNING_CLOSE);
        if (within(t, EVENING_OPEN, EVENING_CLOSE)) return "Open now, closes at " + FMT.format(EVENING_CLOSE);
        if (t.isBefore(MORNING_OPEN)) return "Closed, opens at " + FMT.format(MORNING_OPEN);
        if (t.isBefore(EVENING_OPEN)) return "Closed, opens at " + FMT.format(EVENING_OPEN);
        return "Closed, opens tomorrow at " + FMT.format(MORNING_OPEN);
    }
}