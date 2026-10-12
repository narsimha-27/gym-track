package com.gymtrack.controller;

import com.gymtrack.exception.BusinessRuleException;
import com.gymtrack.model.MembershipPlan;
import com.gymtrack.service.PlanService;
import com.gymtrack.service.PricingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/plans")
public class PlanController {

    private final PlanService planService;
    private final PricingService pricingService;

    public PlanController(PlanService planService, PricingService pricingService) {
        this.planService = planService;
        this.pricingService = pricingService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("plans", planService.findAll());
        model.addAttribute("plan", new MembershipPlan());
        return "plans";
    }

    /** Called by the page while typing the duration; returns the calculated price as plain text. */
    @GetMapping("/price")
    @ResponseBody
    public ResponseEntity<String> price(@RequestParam int months) {
        try {
            return ResponseEntity.ok(pricingService.priceFor(months).toPlainString());
        } catch (BusinessRuleException ex) {
            return ResponseEntity.badRequest().body("");
        }
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("plan") MembershipPlan plan, BindingResult result,
                         Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                planService.create(plan);
                ra.addFlashAttribute("success", "Plan added");
                return "redirect:/plans";
            } catch (BusinessRuleException ex) {
                result.rejectValue("name", "duplicate", ex.getMessage());
            }
        }
        model.addAttribute("plans", planService.findAll());
        return "plans";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            planService.delete(id);
            ra.addFlashAttribute("success", "Plan deleted");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/plans";
    }
}