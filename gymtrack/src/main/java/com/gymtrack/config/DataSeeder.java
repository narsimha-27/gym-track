package com.gymtrack.config;

import com.gymtrack.model.Admin;
import com.gymtrack.model.MembershipPlan;
import com.gymtrack.repository.AdminRepository;
import com.gymtrack.repository.PlanRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Adds the default plans and the admin account the first time the app starts. */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(PlanRepository plans, AdminRepository admins, PasswordEncoder encoder,
                               @Value("${gymtrack.admin.username:admin}") String adminUser,
                               @Value("${gymtrack.admin.password:admin123}") String adminPass) {
        return args -> {
            if (plans.count() == 0) {
                plans.saveAll(List.of(
                        new MembershipPlan("Monthly", 1, new BigDecimal("1000")),
                        new MembershipPlan("Quarterly", 3, new BigDecimal("2700")),
                        new MembershipPlan("Yearly", 12, new BigDecimal("9600"))));
            }
            if (admins.count() == 0) {
                admins.save(new Admin(adminUser, encoder.encode(adminPass)));
            }
        };
    }
}