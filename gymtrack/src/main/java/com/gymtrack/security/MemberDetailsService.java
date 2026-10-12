package com.gymtrack.security;

import com.gymtrack.model.Member;
import com.gymtrack.repository.MemberRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Used only by the member login page. The username is the member's phone number. */
@Service
public class MemberDetailsService implements UserDetailsService {

    private final MemberRepository members;

    public MemberDetailsService(MemberRepository members) {
        this.members = members;
    }

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        Member member = members.findByPhone(phone)
                .filter(m -> m.getPasswordHash() != null)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown member"));
        return User.withUsername(member.getPhone())
                .password(member.getPasswordHash())
                .roles("MEMBER")
                .build();
    }
}