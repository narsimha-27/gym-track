package com.gymtrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class RenewalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Member member;

    private String planName;
    private LocalDateTime requestedAt = LocalDateTime.now();
    private boolean pending = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public boolean isPending() { return pending; }
    public void setPending(boolean pending) { this.pending = pending; }
}