package com.gymtrack.repository;

import com.gymtrack.model.WeightLog;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeightLogRepository extends JpaRepository<WeightLog, Long> {
    Optional<WeightLog> findByMemberIdAndLogDate(Long memberId, LocalDate date);
    List<WeightLog> findTop12ByMemberIdOrderByLogDateDesc(Long memberId);
    void deleteByMemberId(Long memberId);
}