package com.gymtrack.controller;

import com.gymtrack.model.PaymentMode;
import com.gymtrack.service.AnalyticsService;
import com.gymtrack.service.ExportService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/analytics")
public class AnalyticsController {

    private static final int INACTIVE_AFTER_DAYS = 14;

    private final AnalyticsService analytics;
    private final ExportService export;

    public AnalyticsController(AnalyticsService analytics, ExportService export) {
        this.analytics = analytics;
        this.export = export;
    }

    @GetMapping
    public String page(Model model) {
        Map<PaymentMode, BigDecimal> today = analytics.todayCollection();

        model.addAttribute("revenueThisMonth", analytics.revenueThisMonth());
        model.addAttribute("totalRevenue", analytics.totalRevenue());
        model.addAttribute("todayByMode", today);
        model.addAttribute("todayTotal", today.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        chart(model, "rev", analytics.revenueLastMonths(6));
        chart(model, "ci", analytics.checkinsLastDays(7));
        chart(model, "peak", analytics.peakHours(30));
        chart(model, "plan", analytics.revenueByPlan());

        model.addAttribute("inactive", analytics.inactiveMembers(INACTIVE_AFTER_DAYS));
        model.addAttribute("inactiveDays", INACTIVE_AFTER_DAYS);
        return "analytics";
    }

    @GetMapping("/export/members.csv")
    public ResponseEntity<byte[]> exportMembers() {
        return csv("members.csv", export.membersCsv());
    }

    @GetMapping("/export/payments.csv")
    public ResponseEntity<byte[]> exportPayments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return csv("payments.csv", export.paymentsCsv(from, to));
    }

    /** Sends the text as a file download. The BOM at the start makes Excel read it as UTF-8. */
    private ResponseEntity<byte[]> csv(String filename, String content) {
        byte[] bytes = ("\uFEFF" + content).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }

    /** Adds "<name>Labels" and "<name>Values" lists to the model for the page's charts. */
    private void chart(Model model, String name, Map<String, ? extends Number> data) {
        model.addAttribute(name + "Labels", new ArrayList<>(data.keySet()));
        model.addAttribute(name + "Values", data.values().stream().map(Number::doubleValue).toList());
    }
}