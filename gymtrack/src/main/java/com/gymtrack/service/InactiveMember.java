package com.gymtrack.service;

import com.gymtrack.model.Member;
import java.time.LocalDate;

/** A member with a running plan who has not visited recently. lastVisit is null if they never came. */
public record InactiveMember(Member member, LocalDate lastVisit, long daysAway) { }