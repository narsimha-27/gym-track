package com.gymtrack.service;

import com.gymtrack.exception.BusinessRuleException;
import com.gymtrack.exception.ResourceNotFoundException;
import com.gymtrack.model.Member;
import com.gymtrack.repository.AttendanceRepository;
import com.gymtrack.repository.MemberRepository;
import com.gymtrack.repository.MembershipRepository;
import com.gymtrack.repository.RenewalRequestRepository;
import com.gymtrack.repository.WeightLogRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MemberService {

    private final MemberRepository members;
    private final MembershipRepository memberships;
    private final AttendanceRepository attendance;
    private final RenewalRequestRepository renewals;
    private final WeightLogRepository weights;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository members, MembershipRepository memberships,
                         AttendanceRepository attendance, RenewalRequestRepository renewals,
                         WeightLogRepository weights, PasswordEncoder passwordEncoder) {
        this.members = members;
        this.memberships = memberships;
        this.attendance = attendance;
        this.renewals = renewals;
        this.weights = weights;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Member> search(String q) {
        if (q == null || q.isBlank()) {
            return members.findAll(Sort.by("name"));
        }
        String term = q.trim();
        return members.findByNameContainingIgnoreCaseOrPhoneContaining(term, term);
    }

    public List<Member> findAll() {
        return members.findAll(Sort.by("name"));
    }

    public Member getById(Long id) {
        return members.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member " + id + " not found"));
    }

    public Member getByPhone(String phone) {
        return members.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("No member with phone " + phone));
    }

    public Member create(Member m) {
        if (members.existsByPhone(m.getPhone())) {
            throw new BusinessRuleException("A member with this phone number already exists");
        }
        return members.save(m);
    }

    public Member update(Long id, Member form) {
        Member existing = getById(id);
        if (members.existsByPhoneAndIdNot(form.getPhone(), id)) {
            throw new BusinessRuleException("Another member already uses this phone number");
        }
        existing.setName(form.getName());
        existing.setPhone(form.getPhone());
        existing.setEmail(form.getEmail());
        return members.save(existing);
    }

    /** Admin sets or resets the password the member uses to log in to the portal. */
    public void setPortalPassword(Long id, String rawPassword) {
        if (rawPassword == null || rawPassword.trim().length() < 6) {
            throw new BusinessRuleException("Password must be at least 6 characters");
        }
        Member m = getById(id);
        m.setPasswordHash(passwordEncoder.encode(rawPassword.trim()));
        members.save(m);
    }

    /** A logged-in member changes their own password; the current password must be correct. */
    public void changeOwnPassword(String phone, String currentPassword, String newPassword, String confirmPassword) {
        Member m = getByPhone(phone);

        if (currentPassword == null || m.getPasswordHash() == null
                || !passwordEncoder.matches(currentPassword, m.getPasswordHash())) {
            throw new BusinessRuleException("Your current password is incorrect.");
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new BusinessRuleException("New password must be at least 6 characters.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessRuleException("The new password and its confirmation do not match.");
        }
        if (newPassword.equals(currentPassword)) {
            throw new BusinessRuleException("The new password must be different from the current one.");
        }

        m.setPasswordHash(passwordEncoder.encode(newPassword.trim()));
        members.save(m);
    }

    public void delete(Long id) {
        Member m = getById(id);
        attendance.deleteByMemberId(id);
        memberships.deleteByMemberId(id);
        renewals.deleteByMemberId(id);
        weights.deleteByMemberId(id);
        members.delete(m);
    }
}