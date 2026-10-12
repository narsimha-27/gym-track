package com.gymtrack.service;

import com.gymtrack.exception.BusinessRuleException;
import com.gymtrack.model.Attendance;
import com.gymtrack.model.Member;
import com.gymtrack.model.MembershipStatus;
import com.gymtrack.repository.AttendanceRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AttendanceService {

    private final AttendanceRepository attendance;
    private final MemberService memberService;
    private final MembershipService membershipService;

    public AttendanceService(AttendanceRepository attendance, MemberService memberService,
                             MembershipService membershipService) {
        this.attendance = attendance;
        this.memberService = memberService;
        this.membershipService = membershipService;
    }

    public Attendance checkIn(Long memberId) {
        Member member = memberService.getById(memberId);
                if (!GymTimings.isOpen(LocalTime.now())) {
            throw new BusinessRuleException("The gym is closed right now. Timings: " + GymTimings.hoursText() + ".");
        }

        MembershipStatus status = membershipService.statusOf(memberId);
                if (status == MembershipStatus.FROZEN) {
            throw new BusinessRuleException(member.getName() + "'s membership is frozen. Please unfreeze it before checking in.");
        }
        if (status == MembershipStatus.EXPIRED || status == MembershipStatus.NO_PLAN) {
            throw new BusinessRuleException(member.getName() + " has no active membership. Please renew first.");
        }
        if (isCheckedIn(memberId)) {
            throw new BusinessRuleException(member.getName() + " is already checked in.");
        }

        Attendance a = new Attendance();
        a.setMember(member);
        a.setAttendDate(LocalDate.now());
        a.setCheckIn(LocalTime.now().withNano(0));
        return attendance.save(a);
    }

    public Attendance checkOut(Long memberId) {
        Attendance a = attendance
                .findByMemberIdAndAttendDateAndCheckOutIsNull(memberId, LocalDate.now())
                .orElseThrow(() -> new BusinessRuleException("This member is not checked in."));
        a.setCheckOut(LocalTime.now().withNano(0));
        return attendance.save(a);
    }

    @Transactional(readOnly = true)
    public boolean isCheckedIn(Long memberId) {
        return attendance.findByMemberIdAndAttendDateAndCheckOutIsNull(memberId, LocalDate.now()).isPresent();
    }

    @Transactional(readOnly = true)
    public List<Attendance> today() {
        return attendance.findByAttendDateOrderByCheckInDesc(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<Attendance> recent(Long memberId) {
        return attendance.findTop10ByMemberIdOrderByAttendDateDescCheckInDesc(memberId);
    }

    @Transactional(readOnly = true)
    public MonthlySummary monthlySummary(Long memberId, YearMonth ym) {
        List<Attendance> list = attendance.findByMemberIdAndAttendDateBetween(
                memberId, ym.atDay(1), ym.atEndOfMonth());

        int days = (int) list.stream().map(Attendance::getAttendDate).distinct().count();
        Duration total = list.stream()
                .map(Attendance::getDuration)
                .filter(Objects::nonNull)
                .reduce(Duration.ZERO, Duration::plus);
        return new MonthlySummary(ym, days, total);
    }

    /** Day numbers (1-31) of the month on which the member visited. */
    @Transactional(readOnly = true)
    public Set<Integer> attendedDays(Long memberId, YearMonth ym) {
        return attendance.findByMemberIdAndAttendDateBetween(memberId, ym.atDay(1), ym.atEndOfMonth())
                .stream()
                .map(a -> a.getAttendDate().getDayOfMonth())
                .collect(Collectors.toSet());
    }

    /**
     * Number of days in a row the member has visited. If they haven't visited yet today,
     * the streak is still alive and counts back from yesterday.
     */
    @Transactional(readOnly = true)
    public int currentStreak(Long memberId) {
        LocalDate today = LocalDate.now();
        Set<LocalDate> days = attendance
                .findByMemberIdAndAttendDateBetween(memberId, today.minusDays(400), today)
                .stream()
                .map(Attendance::getAttendDate)
                .collect(Collectors.toSet());

        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}