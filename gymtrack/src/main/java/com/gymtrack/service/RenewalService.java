package com.gymtrack.service;

import com.gymtrack.exception.BusinessRuleException;
import com.gymtrack.model.RenewalRequest;
import com.gymtrack.repository.RenewalRequestRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RenewalService {

    private final RenewalRequestRepository requests;
    private final MemberService memberService;
    private final PlanService planService;

    public RenewalService(RenewalRequestRepository requests, MemberService memberService, PlanService planService) {
        this.requests = requests;
        this.memberService = memberService;
        this.planService = planService;
    }

    public RenewalRequest request(Long memberId, Long planId) {
        if (requests.findFirstByMemberIdAndPendingTrue(memberId).isPresent()) {
            throw new BusinessRuleException("You already have a renewal request waiting for the gym.");
        }
        RenewalRequest r = new RenewalRequest();
        r.setMember(memberService.getById(memberId));
        r.setPlanName(planService.getById(planId).getName());
        return requests.save(r);
    }

    @Transactional(readOnly = true)
    public List<RenewalRequest> pending() {
        return requests.findByPendingTrueOrderByRequestedAtAsc();
    }

    @Transactional(readOnly = true)
    public Optional<RenewalRequest> pendingFor(Long memberId) {
        return requests.findFirstByMemberIdAndPendingTrue(memberId);
    }

    /** Called when the admin records a payment: the member's waiting requests are done. */
    public void closeFor(Long memberId) {
        requests.findByMemberIdAndPendingTrue(memberId).forEach(r -> r.setPending(false));
    }
}