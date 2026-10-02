package com.parking.auth.security;

import com.parking.auth.repository.UserRepository;
import com.parking.operator.repository.OperatorRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final OperatorRepository operatorRepo; // Add this

    public CustomUserDetailsService(UserRepository userRepository, 
                                   OperatorRepository operatorRepo) {
        this.userRepository = userRepository;
        this.operatorRepo = operatorRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Check Standard Users Table
        // NOTE: User.Role's enum constants are already "ROLE_USER"/"ROLE_ADMIN"
        // (unlike Operator.OperatorRole below, which isn't prefixed) — this
        // used to prepend "ROLE_" again, granting authorities like
        // "ROLE_ROLE_USER" that never matched any hasRole(...) check.
        var user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            return new org.springframework.security.core.userdetails.User(
                user.get().getUsername(),
                user.get().getPassword(),
                List.of(new SimpleGrantedAuthority(user.get().getRole().name()))
            );
        }

        // 2. Check Operators Table (ID is the "username" here)
        var op = operatorRepo.findByOperatorId(username);
        if (op.isPresent()) {
            return new org.springframework.security.core.userdetails.User(
                op.get().getOperatorId(),
                op.get().getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + op.get().getRole()))
            );
        }

        throw new UsernameNotFoundException("Identity not found in Users or Operators: " + username);
    }
}