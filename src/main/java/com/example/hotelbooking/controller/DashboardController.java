package com.example.hotelbooking.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    
    /**
     * Main dashboard routing based on user role
     * This ONLY redirects, doesn't handle any dashboard logic
     */
    @GetMapping("/dashboard")
    public String showDashboard(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                
                if (role.equals("ROLE_ADMIN")) {
                    return "redirect:/admin/dashboard";
                } else if (role.equals("ROLE_STAFF")) {
                    return "redirect:/staff/dashboard";
                } else if (role.equals("ROLE_USER")) {
                    return "redirect:/user/dashboard";
                }
            }
        }
        return "redirect:/login";
    }
    
    // REMOVE ALL OTHER METHODS:
    // - userDashboard()
    // - staffDashboard()
    // These are handled by UserController and StaffController
}