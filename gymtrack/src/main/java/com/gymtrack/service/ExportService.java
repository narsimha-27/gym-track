package com.gymtrack.service;

import com.gymtrack.model.Member;
import com.gymtrack.model.Membership;
import com.gymtrack.repository.MembershipRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds CSV text (opens in Excel) for the member list and the payments list. */
@Service
@Transactional(readOnly = true)
public class ExportService {

    private final MemberService memberService;
    private final MembershipService membershipService;
    private final MembershipRepository memberships;

    public ExportService(MemberService memberService, MembershipService membershipService,
                         MembershipRepository memberships) {
        this.memberService = memberService;
        this.membershipService = membershipService;
        this.memberships = memberships;
    }

    public String membersCsv() {
        Map<Long, Membership> latest = membershipService.latestForAll();
        StringBuilder sb = new StringBuilder("Name,Phone,Email,Joined,Plan,Valid till,Status\n");
        for (Member m : memberService.findAll()) {
            Membership ms = latest.get(m.getId());
            sb.append(row(m.getName(), m.getPhone(), m.getEmail(), m.getJoinDate(),
                    ms == null ? "" : ms.getPlan().getName(),
                    ms == null ? "" : ms.getEndDate(),
                    ms == null ? "No plan" : ms.getStatus().getLabel()));
        }
        return sb.toString();
    }

    public String paymentsCsv(LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.of(2000, 1, 1);
        LocalDate end = to != null ? to : LocalDate.now();

        List<Membership> list = memberships.findByPaymentDateBetween(start, end).stream()
                .sorted(Comparator.comparing(Membership::getPaymentDate).reversed())
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder("Receipt no,Paid on,Member,Phone,Plan,From,To,Amount,Mode\n");
        for (Membership ms : list) {
            sb.append(row(String.format("GT-%05d", ms.getId()), ms.getPaymentDate(), ms.getMember().getName(),
                    ms.getMember().getPhone(), ms.getPlan().getName(), ms.getStartDate(), ms.getEndDate(),
                    ms.getAmountPaid(), ms.getPaymentMode()));
        }
        return sb.toString();
    }

    private static String row(Object... cells) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) line.append(',');
            line.append(escape(cells[i]));
        }
        return line.append('\n').toString();
    }

    /** Wraps a value in quotes when it contains a comma, quote or line break. */
    private static String escape(Object value) {
        String s = value == null ? "" : String.valueOf(value);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}