package com.gymtrack.service;

import com.gymtrack.exception.BusinessRuleException;
import com.gymtrack.exception.ResourceNotFoundException;
import com.gymtrack.model.MembershipPlan;
import com.gymtrack.repository.MembershipRepository;
import com.gymtrack.repository.PlanRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PlanService {

    private final PlanRepository plans;
    private final MembershipRepository memberships;

    public PlanService(PlanRepository plans, MembershipRepository memberships) {
        this.plans = plans;
        this.memberships = memberships;
    }

    public List<MembershipPlan> findAll() {
        return plans.findAll(Sort.by("durationMonths"));
    }

    public MembershipPlan getById(Long id) {
        return plans.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan " + id + " not found"));
    }

    public MembershipPlan create(MembershipPlan plan) {
        if (plans.existsByNameIgnoreCase(plan.getName().trim())) {
            throw new BusinessRuleException("A plan with this name already exists");
        }
        plan.setName(plan.getName().trim());
        return plans.save(plan);
    }

    public void delete(Long id) {
        MembershipPlan plan = getById(id);
        if (memberships.existsByPlanId(id)) {
            throw new BusinessRuleException("Cannot delete \"" + plan.getName()
                    + "\" because members have already purchased it.");
        }
        plans.delete(plan);
    }
}