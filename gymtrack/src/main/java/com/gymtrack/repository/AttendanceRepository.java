package com.gymtrack.repository;

import com.gymtrack.model.Attendance;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByMemberIdAndAttendDateAndCheckOutIsNull(Long memberId, LocalDate date);
    List<Attendance> findByAttendDateOrderByCheckInDesc(LocalDate date);
    List<Attendance> findTop10ByMemberIdOrderByAttendDateDescCheckInDesc(Long memberId);
    List<Attendance> findByMemberIdAndAttendDateBetween(Long memberId, LocalDate from, LocalDate to);
    List<Attendance> findByAttendDateBetween(LocalDate from, LocalDate to);
    Optional<Attendance> findFirstByMemberIdOrderByAttendDateDesc(Long memberId);
    void deleteByMemberId(Long memberId);
}