package com.example.hotelbooking.service;

import com.example.hotelbooking.model.User;
import com.example.hotelbooking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // Method for registration (used by AuthController)
    @Override
    public User saveUser(User user) {
        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Set default values
        user.setEnabled(true);
        user.setLoyaltyPoints(0);
        
        return userRepository.save(user);
    }
    
    @Override
    public User createUser(User user, String password, String role) {
        // Set encoded password
        user.setPassword(passwordEncoder.encode(password));
        
        return userRepository.save(user);
    }
    
    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @Override
    public List<User> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAll(pageable);
        return userPage.getContent();
    }
    
    @Override
    public List<User> searchUsers(String keyword) {
        return userRepository.searchUsers(keyword);
    }
    
    @Override
    public User updateUser(Long id, User userDetails, String password, String role) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        
        // Update basic info
        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setFullName(userDetails.getFullName());
        user.setPhone(userDetails.getPhone());
        user.setAddress(userDetails.getAddress());
        
        // Update password if provided
        if (password != null && !password.trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    // FIXED: Delete method that handles foreign key constraint
    @Override
    @Transactional
    public void deleteUser(Long id) {
        try {
            // Check if user exists
            User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
            
            // Step 1: Clear all roles (removes entries from user_roles table)
            user.getRoles().clear();
            
            // Step 2: Save the user to apply role removal
            userRepository.save(user);
            
            // Step 3: Flush to ensure database is updated
            userRepository.flush();
            
            // Step 4: Now delete the user
            userRepository.deleteById(id);
            
        } catch (Exception e) {
            throw new RuntimeException("Error deleting user: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setEnabled(!user.isEnabled());
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
    }
    
    @Override
    public boolean usernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }
    
    @Override
    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
    
    @Override
    public long countUsers() {
        return userRepository.count();
    }
    
    @Override
    public List<User> getUsersByRole(String role) {
        // Note: This won't work directly with ManyToMany roles
        // You'll need to implement custom repository query
        throw new UnsupportedOperationException("Method not implemented for ManyToMany roles");
    }
    
    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username;
            
            if (authentication.getPrincipal() instanceof UserDetails) {
                username = ((UserDetails) authentication.getPrincipal()).getUsername();
            } else if (authentication.getPrincipal() instanceof String) {
                username = (String) authentication.getPrincipal();
            } else {
                throw new RuntimeException("Unable to get current user");
            }
            
            return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found in database"));
        }
        throw new RuntimeException("No authenticated user found");
    }
    
    @Override
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
    
    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Override
    public List<User> getActiveUsers() {
        // You need to add this method to UserRepository
        // For now, filter manually
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream()
            .filter(User::isEnabled)
            .toList();
    }
    
    @Override
    public List<User> getInactiveUsers() {
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream()
            .filter(user -> !user.isEnabled())
            .toList();
    }
    
    @Override
    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    @Override
    public void changePasswordWithVerification(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        // Update to new password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    @Override
    public void updateProfile(Long userId, String username, String email) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if new username already exists (if changed)
        if (!user.getUsername().equals(username) && usernameExists(username)) {
            throw new RuntimeException("Username already exists");
        }
        
        // Check if new email already exists (if changed)
        if (!user.getEmail().equals(email) && emailExists(email)) {
            throw new RuntimeException("Email already exists");
        }
        
        user.setUsername(username);
        user.setEmail(email);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
    }
    
    @Override
    public void updateUserProfile(String username, String fullName, String email, 
                                 String phone, String address) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        // Check if email is being changed and already exists
        if (!user.getEmail().equals(email) && emailExists(email)) {
            throw new RuntimeException("Email already exists");
        }
        
        // Update all fields
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setAddress(address);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
    }
}