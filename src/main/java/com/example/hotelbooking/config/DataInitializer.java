package com.example.hotelbooking.config;

import com.example.hotelbooking.model.Role;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.repository.RoleRepository;
import com.example.hotelbooking.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    
    public DataInitializer(UserRepository userRepository, 
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== INITIALIZING DEFAULT DATA ===");
        
        // Create roles
        createRoleIfNotFound("ROLE_ADMIN");
        createRoleIfNotFound("ROLE_STAFF");
        createRoleIfNotFound("ROLE_USER");
        
        // Create admin user if not exists
        if (!userRepository.findByUsername("admin").isPresent()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Administrator");
            admin.setEmail("admin@hotel.com");
            admin.setEnabled(true);
            
            Set<Role> roles = new HashSet<>();
            roleRepository.findByName("ROLE_ADMIN").ifPresent(roles::add);
            roleRepository.findByName("ROLE_STAFF").ifPresent(roles::add);
            roleRepository.findByName("ROLE_USER").ifPresent(roles::add);
            admin.setRoles(roles);
            
            userRepository.save(admin);
            System.out.println("✓ Admin user created");
        }
        
        // Create staff user if not exists
        if (!userRepository.findByUsername("staff").isPresent()) {
            User staff = new User();
            staff.setUsername("staff");
            staff.setPassword(passwordEncoder.encode("staff123"));
            staff.setFullName("Staff Member");
            staff.setEmail("staff@hotel.com");
            staff.setEnabled(true);
            
            Set<Role> roles = new HashSet<>();
            roleRepository.findByName("ROLE_STAFF").ifPresent(roles::add);
            roleRepository.findByName("ROLE_USER").ifPresent(roles::add);
            staff.setRoles(roles);
            
            userRepository.save(staff);
            System.out.println("✓ Staff user created");
        }
        
        // Create regular user if not exists
        if (!userRepository.findByUsername("user").isPresent()) {
            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setFullName("Regular User");
            user.setEmail("user@example.com");
            user.setEnabled(true);
            
            Set<Role> roles = new HashSet<>();
            roleRepository.findByName("ROLE_USER").ifPresent(roles::add);
            user.setRoles(roles);
            
            userRepository.save(user);
            System.out.println("✓ Regular user created");
        }
        
        System.out.println("=== DATA INITIALIZATION COMPLETE ===");
    }
    
    private void createRoleIfNotFound(String roleName) {
        if (!roleRepository.findByName(roleName).isPresent()) {
            Role role = new Role(roleName);
            roleRepository.save(role);
            System.out.println("✓ Created role: " + roleName);
        }
    }
}