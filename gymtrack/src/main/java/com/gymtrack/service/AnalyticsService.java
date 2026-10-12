package com.gymtrack.service;

import com.gymtrack.model.Attendance;
import com.gymtrack.model.Member;
import com.gymtrack.model.Membership;
import com.gymtrack.model.MembershipStatus;
import com.gymtrack.model.PaymentMode;
import com.gymtrack.repository.AttendanceRepository;
import com.gymtrack.repository.MembershipRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Numbers and chart data for the Analytics page. Everything is built with streams and collectors. */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final MembershipRepository memberships;
    private final AttendanceRepository attendance;
    private final MemberService memberService;
    private final MembershipService membershipService;

    public AnalyticsService(MembershipRepository memberships, AttendanceRepository attendance,
                            MemberService memberService, MembershipService membershipService) {
        this.memberships = memberships;
        this.attendance = attendance;
        this.memberService = memberService;
        this.membershipService = membershipService;
    }

    /** Income per month for the last N months (oldest first, months with no income show 0). */
    public Map<String, BigDecimal> revenueLastMonths(int months) {
        YearMonth now = YearMonth.now();
        YearMonth first = now.minusMonths(months - 1L);

        Map<YearMonth, BigDecimal> sums = memberships
                .findByPaymentDateBetween(first.atDay(1), now.atEndOfMonth()).stream()
                .collect(Collectors.groupingBy(
                        m -> YearMonth.from(m.getPaymentDate()),
                        Collectors.reducing(BigDecimal.ZERO, Membership::getAmountPaid, BigDecimal::add)));

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 0; i < months; i++) {
            YearMonth ym = first.plusMonths(i);
            String label = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    + " " + String.format("%02d", ym.getYear() % 100);
            result.put(label, sums.getOrDefault(ym, BigDecimal.ZERO));
        }
        return result;
    }

    public BigDecimal revenueThisMonth() {
        YearMonth now = YearMonth.now();
        return memberships.findByPaymentDateBetween(now.atDay(1), now.atEndOfMonth()).stream()
                .map(Membership::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalRevenue() {
        return memberships.findAll().stream()
                .map(Membership::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Income per plan, biggest first. */
    public Map<String, BigDecimal> revenueByPlan() {
        Map<String, BigDecimal> sums = memberships.findAll().stream()
                .collect(Collectors.groupingBy(
                        m -> m.getPlan().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Membership::getAmountPaid, BigDecimal::add)));

        return sums.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /** Today's payments split by cash / UPI / card (all modes always present). */
    public Map<PaymentMode, BigDecimal> todayCollection() {
        Map<PaymentMode, BigDecimal> result = new EnumMap<>(PaymentMode.class);
        for (PaymentMode mode : PaymentMode.values()) {
            result.put(mode, BigDecimal.ZERO);
        }
        LocalDate today = LocalDate.now();
        memberships.findByPaymentDateBetween(today, today)
                .forEach(ms -> result.merge(ms.getPaymentMode(), ms.getAmountPaid(), BigDecimal::add));
        return result;
    }

    /** Number of visits per day for the last N days (oldest first). */
    public Map<String, Long> checkinsLastDays(int days) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1L);

        Map<LocalDate, Long> counts = attendance.findByAttendDateBetween(from, today).stream()
                .collect(Collectors.groupingBy(Attendance::getAttendDate, Collectors.counting()));

        DateTimeFormatter label = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);
        Map<String, Long> result = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = from.plusDays(i);
            result.put(d.format(label), counts.getOrDefault(d, 0L));
        }
        return result;
    }

    /** Visits by hour of day (6 AM to 8 PM) over the last N days. */
    public Map<String, Long> peakHours(int days) {
        LocalDate today = LocalDate.now();

        Map<Integer, Long> counts = attendance.findByAttendDateBetween(today.minusDays(days - 1L), today).stream()
                .collect(Collectors.groupingBy(a -> a.getCheckIn().getHour(), Collectors.counting()));

        Map<String, Long> result = new LinkedHashMap<>();
        for (int h = 6; h <= 20; h++) {
            String label = (h % 12 == 0 ? 12 : h % 12) + (h < 12 ? "a" : "p");
            result.put(label, counts.getOrDefault(h, 0L));
        }
        return result;
    }

    /** Members with a running plan who have not visited in the last N days. */
    public List<InactiveMember> inactiveMembers(int days) {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.minusDays(days);

        Set<Long> recentVisitors = attendance.findByAttendDateBetween(cutoff, today).stream()
                .map(a -> a.getMember().getId())
                .collect(Collectors.toSet());
        Map<Long, Membership> latest = membershipService.latestForAll();

        List<InactiveMember> result = new ArrayList<>();
        for (Member m : memberService.findAll()) {
            Membership ms = latest.get(m.getId());
            boolean running = ms != null
                    && (ms.getStatus() == MembershipStatus.ACTIVE || ms.getStatus() == MembershipStatus.EXPIRING_SOON);
            boolean joinedBefore = !m.getJoinDate().isAfter(cutoff);

            if (running && joinedBefore && !recentVisitors.contains(m.getId())) {
                LocalDate last = attendance.findFirstByMemberIdOrderByAttendDateDesc(m.getId())
                        .map(Attendance::getAttendDate).orElse(null);
                long away = ChronoUnit.DAYS.between(last != null ? last : m.getJoinDate(), today);
                result.add(new InactiveMember(m, last, away));
            }
        }
        result.sort((a, b) -> Long.compare(b.daysAway(), a.daysAway()));
        return result;
    }
}