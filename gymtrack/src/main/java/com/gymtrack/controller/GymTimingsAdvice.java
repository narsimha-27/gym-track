package com.gymtrack.controller;

import com.gymtrack.service.GymTimings;
import java.time.LocalTime;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Adds the gym timings to every page's model so any template can show them. */
@ControllerAdvice
public class GymTimingsAdvice {

    @ModelAttribute("gymOpen")
    public boolean gymOpen() {
        return GymTimings.isOpen(LocalTime.now());
    }

    @ModelAttribute("gymStatus")
    public String gymStatus() {
        return GymTimings.statusText(LocalTime.now());
    }

    @ModelAttribute("morningHours")
    public String morningHours() {
        return GymTimings.morningText();
    }

    @ModelAttribute("eveningHours")
    public String eveningHours() {
        return GymTimings.eveningText();
    }
}