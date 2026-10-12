package com.gymtrack.repository;

import com.gymtrack.model.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<MembershipPlan, Long> {
    boolean existsByNameIgnoreCase(String name);
}
