package com.gymtrack.controller;

import com.gymtrack.exception.BusinessRuleException;
import com.gymtrack.model.Member;
import com.gymtrack.model.Membership;
import com.gymtrack.model.MembershipStatus;
import com.gymtrack.model.PaymentMode;
import com.gymtrack.service.AttendanceService;
import com.gymtrack.service.MemberService;
import com.gymtrack.service.MembershipService;
import com.gymtrack.service.PlanService;
import com.gymtrack.service.RenewalService;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;
    private final MembershipService membershipService;
    private final PlanService planService;
    private final AttendanceService attendanceService;
    private final RenewalService renewalService;

    public MemberController(MemberService memberService, MembershipService membershipService,
                            PlanService planService, AttendanceService attendanceService,
                            RenewalService renewalService) {
        this.memberService = memberService;
        this.membershipService = membershipService;
        this.planService = planService;
        this.attendanceService = attendanceService;
        this.renewalService = renewalService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        List<Member> members = memberService.search(q);
        Map<Long, Membership> latest = membershipService.latestForAll();

        Map<Long, MembershipStatus> statusMap = new HashMap<>();
        members.forEach(m -> statusMap.put(m.getId(),
                Optional.ofNullable(latest.get(m.getId()))
                        .map(Membership::getStatus)
                        .orElse(MembershipStatus.NO_PLAN)));

        model.addAttribute("members", members);
        model.addAttribute("latestMap", latest);
        model.addAttribute("statusMap", statusMap);
        model.addAttribute("q", q);
        return "members";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("member", new Member());
        return "member-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("member", memberService.getById(id));
        return "member-form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("member") Member member, BindingResult result,
                       RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                Member saved = member.getId() == null
                        ? memberService.create(member)
                        : memberService.update(member.getId(), member);
                ra.addFlashAttribute("success", "Member saved");
                return "redirect:/members/" + saved.getId();
            } catch (BusinessRuleException ex) {
                result.rejectValue("phone", "duplicate", ex.getMessage());
            }
        }
        return "member-form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Member member = memberService.getById(id);
        Optional<Membership> latest = membershipService.latest(id);

        model.addAttribute("member", member);
        model.addAttribute("hasPortalPassword", member.getPasswordHash() != null);
        model.addAttribute("latest", latest.orElse(null));
        model.addAttribute("status", latest.map(Membership::getStatus).orElse(MembershipStatus.NO_PLAN));
        model.addAttribute("history", membershipService.history(id));
        model.addAttribute("recent", attendanceService.recent(id));
        model.addAttribute("summary", attendanceService.monthlySummary(id, YearMonth.now()));
        model.addAttribute("checkedIn", attendanceService.isCheckedIn(id));
        model.addAttribute("plans", planService.findAll());
        model.addAttribute("modes", PaymentMode.values());
        return "member-detail";
    }

    @PostMapping("/{id}/renew")
    public String renew(@PathVariable Long id, @RequestParam Long planId,
                        @RequestParam PaymentMode mode, RedirectAttributes ra) {
        try {
            Membership ms = membershipService.purchase(id, planId, mode);
            renewalService.closeFor(id);
            ra.addFlashAttribute("success", "Payment recorded. Membership valid until " + ms.getEndDate());
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/members/" + id;
    }

    @PostMapping("/{id}/freeze")
    public String freeze(@PathVariable Long id, @RequestParam(required = false) String reason,
                         RedirectAttributes ra) {
        try {
            membershipService.freeze(id, reason);
            ra.addFlashAttribute("success", "Membership frozen. The remaining days are saved until you unfreeze it.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/members/" + id;
    }

    @PostMapping("/{id}/unfreeze")
    public String unfreeze(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Membership ms = membershipService.unfreeze(id);
            ra.addFlashAttribute("success", "Membership resumed. New end date: " + ms.getEndDate());
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/members/" + id;
    }

    @PostMapping("/{id}/password")
    public String setPassword(@PathVariable Long id, @RequestParam String password, RedirectAttributes ra) {
        try {
            memberService.setPortalPassword(id, password);
            ra.addFlashAttribute("success", "Portal password saved. The member can now log in with their phone number.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/members/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        memberService.delete(id);
        ra.addFlashAttribute("success", "Member deleted");
        return "redirect:/members";
    }
}