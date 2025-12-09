package com.example.hotelbooking.repository;

import com.example.hotelbooking.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    
    // Find role by name
    Optional<Role> findByName(String name);
    
    // Check if role exists by name
    boolean existsByName(String name);
    
    // Find all roles by names
    List<Role> findByNameIn(List<String> names);
    
    // Find roles assigned to a specific user
    @Query("SELECT r FROM Role r JOIN r.users u WHERE u.username = :username")
    List<Role> findRolesByUsername(@Param("username") String username);
    
    // Find roles not assigned to a specific user
    @Query("SELECT r FROM Role r WHERE r.id NOT IN " +
           "(SELECT r2.id FROM Role r2 JOIN r2.users u WHERE u.username = :username)")
    List<Role> findAvailableRolesForUser(@Param("username") String username);
    
    // Count users with a specific role
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
    long countUsersByRole(@Param("roleName") String roleName);
    
    // Find role with users (eager loading)
    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.users WHERE r.name = :name")
    Optional<Role> findByNameWithUsers(@Param("name") String name);
}