package com.gymtrack.service;

import com.gymtrack.exception.BusinessRuleException;
import com.gymtrack.exception.ResourceNotFoundException;
import com.gymtrack.model.Member;
import com.gymtrack.model.WeightLog;
import com.gymtrack.repository.MemberRepository;
import com.gymtrack.repository.WeightLogRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProgressService {

    private final WeightLogRepository logs;
    private final MemberRepository members;

    public ProgressService(WeightLogRepository logs, MemberRepository members) {
        this.logs = logs;
        this.members = members;
    }

    /** Saves today's weight (one entry per day). Height is stored once on the member. */
    public void log(Long memberId, BigDecimal weightKg, Integer heightCm) {
        if (weightKg == null || weightKg.compareTo(new BigDecimal("20")) < 0
                || weightKg.compareTo(new BigDecimal("300")) > 0) {
            throw new BusinessRuleException("Enter a weight between 20 and 300 kg.");
        }
        Member member = members.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (heightCm != null) {
            if (heightCm < 100 || heightCm > 250) {
                throw new BusinessRuleException("Enter a height between 100 and 250 cm.");
            }
            member.setHeightCm(heightCm);
            members.save(member);
        }
        if (member.getHeightCm() == null) {
            throw new BusinessRuleException("Enter your height once so BMI can be calculated.");
        }

        WeightLog entry = logs.findByMemberIdAndLogDate(memberId, LocalDate.now()).orElseGet(WeightLog::new);
        entry.setMember(member);
        entry.setLogDate(LocalDate.now());
        entry.setWeightKg(weightKg.setScale(1, RoundingMode.HALF_UP));
        logs.save(entry);
    }

    /** Last 12 entries, oldest first (ready for the chart). */
    @Transactional(readOnly = true)
    public List<WeightLog> history(Long memberId) {
        List<WeightLog> latest = new ArrayList<>(logs.findTop12ByMemberIdOrderByLogDateDesc(memberId));
        Collections.reverse(latest);
        return latest;
    }

    public static BigDecimal bmi(BigDecimal weightKg, Integer heightCm) {
        if (weightKg == null || heightCm == null || heightCm <= 0) return null;
        BigDecimal metres = BigDecimal.valueOf(heightCm).divide(new BigDecimal("100"));
        return weightKg.divide(metres.multiply(metres), 1, RoundingMode.HALF_UP);
    }

    public static String category(BigDecimal bmi) {
        if (bmi == null) return "-";
        double v = bmi.doubleValue();
        if (v < 18.5) return "Underweight";
        if (v < 25) return "Normal";
        if (v < 30) return "Overweight";
        return "Obese";
    }
}