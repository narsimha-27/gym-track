package com.gymtrack.repository;

import com.gymtrack.model.RenewalRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RenewalRequestRepository extends JpaRepository<RenewalRequest, Long> {
    List<RenewalRequest> findByPendingTrueOrderByRequestedAtAsc();
    Optional<RenewalRequest> findFirstByMemberIdAndPendingTrue(Long memberId);
    List<RenewalRequest> findByMemberIdAndPendingTrue(Long memberId);
    void deleteByMemberId(Long memberId);
}