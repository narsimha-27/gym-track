package com.gymtrack.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public enum MembershipStatus {
    ACTIVE("Active"),
    EXPIRING_SOON("Expiring soon"),
    EXPIRED("Expired"),
    FROZEN("Frozen"),
    NO_PLAN("No plan");

    public static final int EXPIRY_WARNING_DAYS = 7;

    private final String label;

    MembershipStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getCss() {
        return name().toLowerCase();
    }

    /** Works out the status from a membership end date (a frozen plan is handled in Membership). */
    public static MembershipStatus of(LocalDate endDate, LocalDate today) {
        if (endDate == null) return NO_PLAN;
        if (endDate.isBefore(today)) return EXPIRED;
        long daysLeft = ChronoUnit.DAYS.between(today, endDate);
        return daysLeft <= EXPIRY_WARNING_DAYS ? EXPIRING_SOON : ACTIVE;
    }
}