package com.gymtrack.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** One purchase / renewal of a plan by a member (also acts as the payment record). */
@Entity
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Member member;

    @ManyToOne(optional = false)
    private MembershipPlan plan;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate paymentDate;
    private BigDecimal amountPaid;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    /** Date the plan was paused; null while the plan is running normally. */
    private LocalDate frozenOn;
    private String freezeReason;

    /** Total days this plan has been frozen so far (Integer so older rows with NULL still load). */
    private Integer frozenDaysTotal;

    public boolean isFrozen() {
        return frozenOn != null;
    }

    public MembershipStatus getStatus() {
        if (isFrozen()) return MembershipStatus.FROZEN;
        return MembershipStatus.of(endDate, LocalDate.now());
    }

    /** Days remaining. While frozen the count is held at the day the plan was paused. */
    public long getDaysLeft() {
        LocalDate from = isFrozen() ? frozenOn : LocalDate.now();
        return ChronoUnit.DAYS.between(from, endDate);
    }

    /** Days the current freeze has lasted so far (0 if not frozen). */
    public long getDaysFrozenSoFar() {
        return isFrozen() ? ChronoUnit.DAYS.between(frozenOn, LocalDate.now()) : 0;
    }

    /** Length of this membership in days (start and end day both count). */
    public long getTotalDays() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /** Share of the membership still remaining, 0 to 100 (used by the ring on the member page). */
    public int getPercentLeft() {
        long total = getTotalDays();
        if (total <= 0) return 0;
        long pct = Math.round(getDaysLeft() * 100.0 / total);
        return (int) Math.max(0, Math.min(100, pct));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public MembershipPlan getPlan() { return plan; }
    public void setPlan(MembershipPlan plan) { this.plan = plan; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
    public PaymentMode getPaymentMode() { return paymentMode; }
    public void setPaymentMode(PaymentMode paymentMode) { this.paymentMode = paymentMode; }
    public LocalDate getFrozenOn() { return frozenOn; }
    public void setFrozenOn(LocalDate frozenOn) { this.frozenOn = frozenOn; }
    public String getFreezeReason() { return freezeReason; }
    public void setFreezeReason(String freezeReason) { this.freezeReason = freezeReason; }
    public int getFrozenDaysTotal() { return frozenDaysTotal == null ? 0 : frozenDaysTotal; }
    public void setFrozenDaysTotal(int days) { this.frozenDaysTotal = days; }
}