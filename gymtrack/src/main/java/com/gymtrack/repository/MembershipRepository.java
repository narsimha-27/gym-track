package com.gymtrack.repository;

import com.gymtrack.model.Membership;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    List<Membership> findByMemberIdOrderByEndDateDesc(Long memberId);
    Optional<Membership> findFirstByMemberIdOrderByEndDateDesc(Long memberId);
    void deleteByMemberId(Long memberId);
    boolean existsByPlanId(Long planId);
    List<Membership> findByPaymentDateBetween(LocalDate from, LocalDate to);
}