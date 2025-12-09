package com.example.hotelbooking.service;

import com.example.hotelbooking.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsService {
    
    private final UserRepository userRepository;
    
    public StatisticsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public List<RoleStatistic> getRoleStatistics() {
        List<RoleStatistic> stats = new ArrayList<>();
        
        // Count users with ADMIN role
        long adminCount = userRepository.findAll().stream()
            .filter(user -> user.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName())))
            .count();
        stats.add(new RoleStatistic("ROLE_ADMIN", adminCount));
        
        // Count users with STAFF role
        long staffCount = userRepository.findAll().stream()
            .filter(user -> user.getRoles().stream()
                .anyMatch(role -> "ROLE_STAFF".equals(role.getName())))
            .count();
        stats.add(new RoleStatistic("ROLE_STAFF", staffCount));
        
        // Count users with USER role
        long userCount = userRepository.findAll().stream()
            .filter(user -> user.getRoles().stream()
                .anyMatch(role -> "ROLE_USER".equals(role.getName())))
            .count();
        stats.add(new RoleStatistic("ROLE_USER", userCount));
        
        return stats;
    }
    
    // Inner class for role statistics
    public static class RoleStatistic {
        private final String roleName;
        private final long count;
        
        public RoleStatistic(String roleName, long count) {
            this.roleName = roleName;
            this.count = count;
        }
        
        public String getRoleName() {
            return roleName;
        }
        
        public long getCount() {
            return count;
        }
    }
}