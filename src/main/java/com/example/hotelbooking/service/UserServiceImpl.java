package com.example.hotelbooking.service;

import com.example.hotelbooking.model.Role;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.repository.RoleRepository;
import com.example.hotelbooking.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepo, RoleRepository roleRepo,
                           PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Using orElse to handle Optional elegantly
        Role roleUser = roleRepo.findByName("ROLE_USER")
                .orElseGet(() -> roleRepo.save(new Role("ROLE_USER")));
        
        user.setRoles(Set.of(roleUser));
        return userRepo.save(user);
    }
}