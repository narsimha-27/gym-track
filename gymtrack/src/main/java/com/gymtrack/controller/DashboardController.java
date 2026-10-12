package com.gymtrack.controller;

import com.gymtrack.model.Membership;
import com.gymtrack.model.MembershipStatus;
import com.gymtrack.service.AttendanceService;
import com.gymtrack.service.MemberService;
import com.gymtrack.service.MembershipService;
import com.gymtrack.service.RenewalService;
import java.security.Principal;
import java.util.Collection;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final MemberService memberService;
    private final MembershipService membershipService;
    private final AttendanceService attendanceService;
    private final RenewalService renewalService;

    public DashboardController(MemberService memberService, MembershipService membershipService,
                               AttendanceService attendanceService, RenewalService renewalService) {
        this.memberService = memberService;
        this.membershipService = membershipService;
        this.attendanceService = attendanceService;
        this.renewalService = renewalService;
    }

    @GetMapping("/")
    public String dashboard(Model model, Principal principal) {
        Collection<Membership> latest = membershipService.latestForAll().values();

        long active = latest.stream()
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE
                          || m.getStatus() == MembershipStatus.EXPIRING_SOON)
                .count();
        long frozen = latest.stream().filter(m -> m.getStatus() == MembershipStatus.FROZEN).count();

        model.addAttribute("adminName", principal.getName());
        model.addAttribute("totalMembers", memberService.findAll().size());
        model.addAttribute("activeMembers", active);
        model.addAttribute("frozenCount", frozen);
        model.addAttribute("todayCount", attendanceService.today().size());
        model.addAttribute("expiringSoon", membershipService.expiringSoon());
        model.addAttribute("expired", membershipService.expired());
        model.addAttribute("warningDays", MembershipStatus.EXPIRY_WARNING_DAYS);
        model.addAttribute("renewalRequests", renewalService.pending());
        return "dashboard";
    }
}