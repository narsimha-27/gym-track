package com.gymtrack.controller;

import com.gymtrack.exception.BusinessRuleException;
import com.gymtrack.model.Member;
import com.gymtrack.model.Membership;
import com.gymtrack.model.MembershipStatus;
import com.gymtrack.model.WeightLog;
import com.gymtrack.service.AttendanceService;
import com.gymtrack.service.CalendarCell;
import com.gymtrack.service.MemberService;
import com.gymtrack.service.MembershipService;
import com.gymtrack.service.MonthlySummary;
import com.gymtrack.service.PlanService;
import com.gymtrack.service.ProgressService;
import com.gymtrack.service.RenewalService;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Pages for a logged-in member. The member is taken from the login, never from the URL. */
@Controller
@RequestMapping("/portal")
public class PortalController {

    /** Visits per month the member is aiming for. */
    private static final int MONTHLY_GOAL = 12;

    private final MemberService memberService;
    private final MembershipService membershipService;
    private final AttendanceService attendanceService;
    private final PlanService planService;
    private final RenewalService renewalService;
    private final ProgressService progressService;

    public PortalController(MemberService memberService, MembershipService membershipService,
                            AttendanceService attendanceService, PlanService planService,
                            RenewalService renewalService, ProgressService progressService) {
        this.memberService = memberService;
        this.membershipService = membershipService;
        this.attendanceService = attendanceService;
        this.planService = planService;
        this.renewalService = renewalService;
        this.progressService = progressService;
    }

    @GetMapping
    public String home(Principal principal, Model model) {
        Member member = memberService.getByPhone(principal.getName());
        Long id = member.getId();
        YearMonth month = YearMonth.now();
        Optional<Membership> latest = membershipService.latest(id);
        MonthlySummary summary = attendanceService.monthlySummary(id, month);

        model.addAttribute("member", member);
        model.addAttribute("latest", latest.orElse(null));
        model.addAttribute("status", latest.map(Membership::getStatus).orElse(MembershipStatus.NO_PLAN));
        model.addAttribute("history", membershipService.history(id));
        model.addAttribute("recent", attendanceService.recent(id));
        model.addAttribute("summary", summary);

        // streak, goal and calendar
        model.addAttribute("streak", attendanceService.currentStreak(id));
        model.addAttribute("goal", MONTHLY_GOAL);
        model.addAttribute("goalPercent", Math.min(100, summary.daysPresent() * 100 / MONTHLY_GOAL));
        model.addAttribute("calendar", CalendarCell.build(month, attendanceService.attendedDays(id, month), LocalDate.now()));

        // renewal request
        model.addAttribute("plans", planService.findAll());
        model.addAttribute("pendingRequest", renewalService.pendingFor(id).orElse(null));

        // body progress
        List<WeightLog> logs = progressService.history(id);
        DateTimeFormatter label = DateTimeFormatter.ofPattern("dd MMM");
        model.addAttribute("chartLabels", logs.stream().map(l -> l.getLogDate().format(label)).toList());
        model.addAttribute("chartValues", logs.stream().map(l -> l.getWeightKg().doubleValue()).toList());

        BigDecimal latestWeight = logs.isEmpty() ? null : logs.get(logs.size() - 1).getWeightKg();
        BigDecimal bmi = ProgressService.bmi(latestWeight, member.getHeightCm());
        String changeText = null;
        if (logs.size() > 1) {
            BigDecimal diff = latestWeight.subtract(logs.get(0).getWeightKg());
            changeText = String.format("%+.1f kg", diff.doubleValue());
        }
        model.addAttribute("latestWeight", latestWeight);
        model.addAttribute("bmi", bmi);
        model.addAttribute("bmiCategory", ProgressService.category(bmi));
        model.addAttribute("weightChange", changeText);
        return "portal";
    }

    @PostMapping("/password")
    public String changePassword(Principal principal,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes ra) {
        try {
            memberService.changeOwnPassword(principal.getName(), currentPassword, newPassword, confirmPassword);
            ra.addFlashAttribute("success", "Your password has been changed.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/portal";
    }

    @PostMapping("/renewal")
    public String requestRenewal(Principal principal, @RequestParam Long planId, RedirectAttributes ra) {
        try {
            Member me = memberService.getByPhone(principal.getName());
            renewalService.request(me.getId(), planId);
            ra.addFlashAttribute("success", "Renewal request sent. The gym will contact you to collect the payment.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/portal";
    }

    @PostMapping("/progress")
    public String logProgress(Principal principal, @RequestParam BigDecimal weightKg,
                              @RequestParam(required = false) Integer heightCm, RedirectAttributes ra) {
        try {
            Member me = memberService.getByPhone(principal.getName());
            progressService.log(me.getId(), weightKg, heightCm);
            ra.addFlashAttribute("success", "Weight saved.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/portal";
    }
}