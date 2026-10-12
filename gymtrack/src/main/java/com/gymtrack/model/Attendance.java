package com.gymtrack.model;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Member member;

    private LocalDate attendDate;
    private LocalTime checkIn;
    private LocalTime checkOut; // null while the member is still inside

    public Duration getDuration() {
        return checkOut == null ? null : Duration.between(checkIn, checkOut);
    }

    public String getDurationText() {
        Duration d = getDuration();
        if (d == null) return "Inside";
        long mins = d.toMinutes();
        return (mins / 60) + "h " + String.format("%02d", mins % 60) + "m";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public LocalDate getAttendDate() { return attendDate; }
    public void setAttendDate(LocalDate attendDate) { this.attendDate = attendDate; }
    public LocalTime getCheckIn() { return checkIn; }
    public void setCheckIn(LocalTime checkIn) { this.checkIn = checkIn; }
    public LocalTime getCheckOut() { return checkOut; }
    public void setCheckOut(LocalTime checkOut) { this.checkOut = checkOut; }
}
