package com.example.hotelbooking.service;

import com.example.hotelbooking.model.User;
import java.util.List;
import java.util.Optional;

public interface UserService {
    
    // User CRUD operations
    User saveUser(User user); // For registration
    User createUser(User user, String password, String role); // For admin creation
    Optional<User> getUserById(Long id);
    List<User> getAllUsers();
    List<User> getAllUsers(int page, int size);
    List<User> searchUsers(String keyword);
    User updateUser(Long id, User user, String password, String role);
    void deleteUser(Long id);
    void toggleUserStatus(Long id);
    
    // Validation methods
    boolean usernameExists(String username);
    boolean emailExists(String email);
    long countUsers();
    
    // Additional methods
    List<User> getUsersByRole(String role);
    User getCurrentUser();
    Optional<User> getUserByUsername(String username);
    Optional<User> getUserByEmail(String email);
    List<User> getActiveUsers();
    List<User> getInactiveUsers();
    
    // Password and profile management
    void changePassword(Long userId, String newPassword);
    void updateProfile(Long userId, String username, String email); // This is the method signature
    
    // ADD THESE NEW METHODS FOR USER DASHBOARD
    User findByUsername(String username); // For controller
    
    void updateUserProfile(String username, String fullName, String email, String phone, String address);
    
    // New method for password change with verification
    void changePasswordWithVerification(String username, String currentPassword, String newPassword);
}