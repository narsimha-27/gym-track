package com.gymtrack.controller;

import com.gymtrack.exception.BusinessRuleException;
import com.gymtrack.service.AttendanceService;
import com.gymtrack.service.MemberService;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final MemberService memberService;

    public AttendanceController(AttendanceService attendanceService, MemberService memberService) {
        this.attendanceService = attendanceService;
        this.memberService = memberService;
    }

    @GetMapping
    public String today(Model model) {
        model.addAttribute("records", attendanceService.today());
        model.addAttribute("members", memberService.findAll());
        model.addAttribute("today", LocalDate.now());
        return "attendance";
    }

    @PostMapping("/checkin")
    public String checkIn(@RequestParam Long memberId, @RequestParam(required = false) String returnTo,
                          RedirectAttributes ra) {
        try {
            attendanceService.checkIn(memberId);
            ra.addFlashAttribute("success", "Checked in");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return redirectTo(returnTo);
    }

    @PostMapping("/checkout")
    public String checkOut(@RequestParam Long memberId, @RequestParam(required = false) String returnTo,
                           RedirectAttributes ra) {
        try {
            attendanceService.checkOut(memberId);
            ra.addFlashAttribute("success", "Checked out");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return redirectTo(returnTo);
    }

    /** Only allow redirects to pages inside this app. */
    private String redirectTo(String returnTo) {
        boolean safe = returnTo != null && returnTo.startsWith("/") && !returnTo.startsWith("//");
        return "redirect:" + (safe ? returnTo : "/attendance");
    }
}
