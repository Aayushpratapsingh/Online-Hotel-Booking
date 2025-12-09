package com.example.hotelbooking.controller;

import com.example.hotelbooking.model.User;
import com.example.hotelbooking.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final UserService userService;
    
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                       @RequestParam(value = "logout", required = false) String logout,
                       Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password!");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully!");
        }
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String saveUser(@ModelAttribute User user) {
        userService.saveUser(user);
        return "redirect:/login?registered=true";
    }

    @GetMapping("/home")
    public String home(Model model) {
        // Add statistics for the home page
        model.addAttribute("totalUsers", 15); 
        model.addAttribute("totalRooms", 50); 
        model.addAttribute("activeBookings", 12);
        model.addAttribute("todayRevenue", 2450.00);
        model.addAttribute("newUsersToday", 3);
        model.addAttribute("newBookingsToday", 5);
        model.addAttribute("checkInsToday", 2);
        model.addAttribute("checkOutsToday", 1);
        return "home";
    }

    @GetMapping("/admin")
    public String admin() { 
        return "admin"; 
    }

    @GetMapping("/staff")
    public String staff() { 
        return "staff"; 
    }

    @GetMapping("/user")
    public String userPage() { 
        return "user"; 
    }
}