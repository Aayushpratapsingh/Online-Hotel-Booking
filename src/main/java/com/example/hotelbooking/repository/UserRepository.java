package com.example.hotelbooking.repository;

import com.example.hotelbooking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    
    // Check if username exists
    default boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }
    
    // Check if email exists
    default boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }
    
    // For user search
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> searchUsers(String query);
    
    // Find by enabled status (add these methods)
    List<User> findByEnabled(boolean enabled);
    
    // Add this method to support UserServiceImpl
    @Query("SELECT u FROM User u WHERE u.enabled = true")
    List<User> findActiveUsers();
    
    @Query("SELECT u FROM User u WHERE u.enabled = false")
    List<User> findInactiveUsers();
}