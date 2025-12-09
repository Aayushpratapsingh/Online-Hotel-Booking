package com.example.hotelbooking.repository;

import com.example.hotelbooking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    
    // Basic CRUD operations are inherited from JpaRepository
    
    // Find user by username
    Optional<User> findByUsername(String username);
    
    // Find user by email
    Optional<User> findByEmail(String email);
    
    // Check if username exists
    boolean existsByUsername(String username);
    
    // Check if email exists
    boolean existsByEmail(String email);
    
    // Find users by enabled status
    List<User> findByEnabled(boolean enabled);
    
    // Find users by full name containing (case-insensitive)
    List<User> findByFullNameContainingIgnoreCase(String name);
    
    // Find users by role name
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);
    
    // Count active users
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true")
    long countActiveUsers();
    
    // Search users by username, full name, or email
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchUsers(@Param("keyword") String keyword);
    
    // Find users with specific role IDs
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.id IN :roleIds")
    List<User> findByRoleIds(@Param("roleIds") List<Long> roleIds);
    
    // Find users without any role
    @Query("SELECT u FROM User u WHERE u.roles IS EMPTY")
    List<User> findUsersWithoutRoles();
    
    // Find users created after specific date (if you have createdDate field)
    // @Query("SELECT u FROM User u WHERE u.createdDate > :date")
    // List<User> findUsersCreatedAfter(@Param("date") LocalDateTime date);
}