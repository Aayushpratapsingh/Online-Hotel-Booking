package com.example.hotelbooking.service;

import com.example.hotelbooking.model.User;
import com.example.hotelbooking.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("=== CUSTOM USER DETAILS SERVICE ===");
        System.out.println("Looking for user: " + username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                System.out.println("User NOT found: " + username);
                return new UsernameNotFoundException("User not found: " + username);
            });
        
        System.out.println("User found: " + user.getUsername());
        System.out.println("Password hash: " + user.getPassword());
        System.out.println("Enabled: " + user.isEnabled());
        System.out.println("Roles count: " + (user.getRoles() != null ? user.getRoles().size() : 0));
        
        if (!user.isEnabled()) {
            System.out.println("User is disabled: " + username);
            throw new UsernameNotFoundException("User is disabled: " + username);
        }
        
        return org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPassword())
            .authorities(user.getRoles().stream()
                .map(role -> {
                    System.out.println("Granting authority: " + role.getName());
                    return new org.springframework.security.core.authority.SimpleGrantedAuthority(role.getName());
                })
                .toArray(org.springframework.security.core.authority.SimpleGrantedAuthority[]::new))
            .build();
    }
}