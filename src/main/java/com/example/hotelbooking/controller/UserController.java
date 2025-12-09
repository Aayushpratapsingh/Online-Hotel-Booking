package com.example.hotelbooking.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;

@Controller
@RequestMapping("/user")
@PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')")
public class UserController {
    
    @GetMapping("/dashboard")
    public String userDashboard(Model model, Principal principal) {
        model.addAttribute("username", principal.getName());
        return "user/dashboard";
    }
    
    @GetMapping("/bookings")
    public String userBookings() {
        return "user/bookings";
    }
    
    @GetMapping("/profile")
    public String userProfile() {
        return "user/profile";
    }
}