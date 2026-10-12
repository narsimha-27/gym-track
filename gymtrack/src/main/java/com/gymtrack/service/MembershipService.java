package com.gymtrack.service;

import com.gymtrack.exception.BusinessRuleException;
import com.gymtrack.exception.ResourceNotFoundException;
import com.gymtrack.model.Member;
import com.gymtrack.model.Membership;
import com.gymtrack.model.MembershipPlan;
import com.gymtrack.model.MembershipStatus;
import com.gymtrack.model.PaymentMode;
import com.gymtrack.repository.MembershipRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MembershipService {

    private final MembershipRepository memberships;
    private final MemberService memberService;
    private final PlanService planService;

    public MembershipService(MembershipRepository memberships, MemberService memberService, PlanService planService) {
        this.memberships = memberships;
        this.memberService = memberService;
        this.planService = planService;
    }

    /**
     * Buys or renews a plan. If the member still has an unexpired plan, the new
     * one starts the day after it ends (no days are lost); otherwise it starts today.
     */
    public Membership purchase(Long memberId, Long planId, PaymentMode mode) {
        Member member = memberService.getById(memberId);
        MembershipPlan plan = planService.getById(planId);
        LocalDate today = LocalDate.now();

        latest(memberId).filter(Membership::isFrozen).ifPresent(f -> {
            throw new BusinessRuleException("This membership is frozen. Unfreeze it before recording a new payment.");
        });

        LocalDate start = latest(memberId)
                .map(Membership::getEndDate)
                .filter(end -> !end.isBefore(today))
                .map(end -> end.plusDays(1))
                .orElse(today);
        LocalDate end = start.plusMonths(plan.getDurationMonths()).minusDays(1);

        Membership ms = new Membership();
        ms.setMember(member);
        ms.setPlan(plan);
        ms.setStartDate(start);
        ms.setEndDate(end);
        ms.setPaymentDate(today);
        ms.setAmountPaid(plan.getPrice());
        ms.setPaymentMode(mode);
        return memberships.save(ms);
    }

    /** Pauses the member's current plan. The days left are held until it is unfrozen. */
    public Membership freeze(Long memberId, String reason) {
        Membership ms = latest(memberId)
                .orElseThrow(() -> new BusinessRuleException("This member has no membership to freeze."));
        if (ms.isFrozen()) {
            throw new BusinessRuleException("This membership is already frozen.");
        }
        if (ms.getStatus() == MembershipStatus.EXPIRED) {
            throw new BusinessRuleException("An expired membership cannot be frozen. Please renew it instead.");
        }
        ms.setFrozenOn(LocalDate.now());
        ms.setFreezeReason(reason == null || reason.isBlank() ? null : reason.trim());
        return memberships.save(ms);
    }

    /** Resumes the plan and moves the end date forward by the number of days it was frozen. */
    public Membership unfreeze(Long memberId) {
        Membership ms = latest(memberId)
                .filter(Membership::isFrozen)
                .orElseThrow(() -> new BusinessRuleException("This membership is not frozen."));

        long days = ChronoUnit.DAYS.between(ms.getFrozenOn(), LocalDate.now());
        ms.setEndDate(ms.getEndDate().plusDays(days));
        ms.setFrozenDaysTotal(ms.getFrozenDaysTotal() + (int) days);
        ms.setFrozenOn(null);
        ms.setFreezeReason(null);
        return memberships.save(ms);
    }

    @Transactional(readOnly = true)
    public Membership getById(Long id) {
        return memberships.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found"));
    }

    @Transactional(readOnly = true)
    public Optional<Membership> latest(Long memberId) {
        return memberships.findFirstByMemberIdOrderByEndDateDesc(memberId);
    }

    @Transactional(readOnly = true)
    public List<Membership> history(Long memberId) {
        return memberships.findByMemberIdOrderByEndDateDesc(memberId);
    }

    @Transactional(readOnly = true)
    public MembershipStatus statusOf(Long memberId) {
        return latest(memberId).map(Membership::getStatus).orElse(MembershipStatus.NO_PLAN);
    }

    /** Latest membership of every member, keyed by member id (built with streams). */
    @Transactional(readOnly = true)
    public Map<Long, Membership> latestForAll() {
        return memberships.findAll().stream()
                .collect(Collectors.toMap(
                        m -> m.getMember().getId(),
                        Function.identity(),
                        (a, b) -> a.getEndDate().isAfter(b.getEndDate()) ? a : b));
    }

    @Transactional(readOnly = true)
    public List<Membership> expiringSoon() {
        return latestForAll().values().stream()
                .filter(m -> m.getStatus() == MembershipStatus.EXPIRING_SOON)
                .sorted(Comparator.comparing(Membership::getEndDate))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Membership> expired() {
        return latestForAll().values().stream()
                .filter(m -> m.getStatus() == MembershipStatus.EXPIRED)
                .sorted(Comparator.comparing(Membership::getEndDate).reversed())
                .toList();
    }
}