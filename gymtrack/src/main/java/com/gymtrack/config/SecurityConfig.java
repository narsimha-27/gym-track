package com.gymtrack.config;

import com.gymtrack.security.AdminDetailsService;
import com.gymtrack.security.MemberDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Two separate security "chains":
 *   1. Member chain: everything under /portal/**  (login page /portal/login)
 *   2. Admin chain:  everything else              (login page /admin/login)
 * Each chain has its own login page and only accepts its own kind of account.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain memberChain(HttpSecurity http, MemberDetailsService members,
                                    PasswordEncoder encoder) throws Exception {
        http
            .securityMatcher("/portal/**")
            .authenticationProvider(provider(members, encoder))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/portal/login").permitAll()
                .anyRequest().hasRole("MEMBER"))
            .formLogin(form -> form
                .loginPage("/portal/login")
                .loginProcessingUrl("/portal/login")
                .defaultSuccessUrl("/portal", true)
                .failureUrl("/portal/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/portal/logout")
                .logoutSuccessUrl("/portal/login?logout")
                .permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain adminChain(HttpSecurity http, AdminDetailsService admins,
                                   PasswordEncoder encoder) throws Exception {
        http
            .authenticationProvider(provider(admins, encoder))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/admin/login", "/error", "/manifest.json", "/icon.svg").permitAll()
                .anyRequest().hasRole("ADMIN"))
            .formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/admin/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login?logout")
                .permitAll());
        return http.build();
    }

    private DaoAuthenticationProvider provider(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(users);
        p.setPasswordEncoder(encoder);
        return p;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}