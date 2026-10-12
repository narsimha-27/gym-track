package com.gymtrack.security;

import com.gymtrack.model.Admin;
import com.gymtrack.repository.AdminRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Used only by the admin login page. */
@Service
public class AdminDetailsService implements UserDetailsService {

    private final AdminRepository admins;

    public AdminDetailsService(AdminRepository admins) {
        this.admins = admins;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Admin admin = admins.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown admin"));
        return User.withUsername(admin.getUsername())
                .password(admin.getPasswordHash())
                .roles("ADMIN")
                .build();
    }
}