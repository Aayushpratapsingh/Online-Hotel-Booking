package com.example.hotelbooking.controller;

import com.example.hotelbooking.model.Booking;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.service.BookingService;
import com.example.hotelbooking.service.BookingService.DashboardStatisticsWithSpent;
import com.example.hotelbooking.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/user")
public class UserController {
    
    private final UserService userService;
    private final BookingService bookingService;
    private final PasswordEncoder passwordEncoder;
    
    public UserController(UserService userService, 
                         BookingService bookingService,
                         PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.bookingService = bookingService;
        this.passwordEncoder = passwordEncoder;
    }
    
    // ========== DASHBOARD ==========
    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String userDashboard(Model model, Principal principal) {
        try {
            User user = userService.findByUsername(principal.getName());
            
            // Get dashboard statistics WITH TOTAL SPENT
            DashboardStatisticsWithSpent stats = bookingService.getUserDashboardStatsWithSpent(user);
            
            // Add attributes for dashboard
            model.addAttribute("userFullName", user.getFullName() != null ? user.getFullName() : user.getUsername());
            model.addAttribute("user", user);
            model.addAttribute("totalBookings", stats.getTotalBookings());
            model.addAttribute("upcomingBookings", stats.getUpcomingBookings());
            model.addAttribute("completedBookings", stats.getCompletedBookings());
            model.addAttribute("totalSpent", stats.getTotalSpent());
            model.addAttribute("loyaltyPoints", user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0);
            model.addAttribute("recentBookings", bookingService.getRecentBookings(user, 5));
            model.addAttribute("isAuthenticated", true);
            
            return "user/dashboard";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error loading dashboard: " + e.getMessage());
            return "error";
        }
    }
    
    // ========== PROFILE ==========
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public String userProfile(Model model, Principal principal) {
        try {
            User user = userService.findByUsername(principal.getName());
            
            // Calculate member since years
            long memberSinceYears = 1;
            if (user.getCreatedAt() != null) {
                LocalDate createdAtDate = user.getCreatedAt().toLocalDate();
                memberSinceYears = Period.between(createdAtDate, LocalDate.now()).getYears();
                if (memberSinceYears < 1) memberSinceYears = 1;
            }
            
            // Get booking stats
            DashboardStatisticsWithSpent stats = bookingService.getUserDashboardStatsWithSpent(user);
            
            model.addAttribute("user", user);
            model.addAttribute("totalBookings", stats.getTotalBookings());
            model.addAttribute("loyaltyPoints", user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0);
            model.addAttribute("memberSince", memberSinceYears);
            model.addAttribute("isAuthenticated", true);
            model.addAttribute("userFullName", user.getFullName() != null ? user.getFullName() : user.getUsername());
            
            return "user/profile";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error loading profile: " + e.getMessage());
            return "error";
        }
    }
    
    @PostMapping("/profile/update")
    @PreAuthorize("isAuthenticated()")
    public String updateProfile(@RequestParam("fullName") String fullName,
                               @RequestParam("email") String email,
                               @RequestParam("phone") String phone,
                               @RequestParam("address") String address,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserProfile(principal.getName(), fullName, email, phone, address);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating profile: " + e.getMessage());
        }
        
        return "redirect:/user/profile";
    }
    
    // ========== SETTINGS ==========
    @GetMapping("/settings")
    @PreAuthorize("isAuthenticated()")
    public String userSettings(Model model, Principal principal) {
        try {
            User user = userService.findByUsername(principal.getName());
            
            model.addAttribute("userFullName", user.getFullName() != null ? user.getFullName() : user.getUsername());
            model.addAttribute("user", user);
            model.addAttribute("isAuthenticated", true);
            
            return "user/settings";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error loading settings: " + e.getMessage());
            return "error";
        }
    }
    
    @PostMapping("/settings/change-password")
    @PreAuthorize("isAuthenticated()")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                @RequestParam("newPassword") String newPassword,
                                @RequestParam("confirmPassword") String confirmPassword,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            // Get current user
            User user = userService.findByUsername(principal.getName());
            
            // Validate
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "New passwords do not match!");
                return "redirect:/user/settings";
            }
            
            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("error", "Current password is incorrect!");
                return "redirect:/user/settings";
            }
            
            // Change password using service method (needs userId, not current password verification)
            userService.changePassword(user.getId(), newPassword);
            
            redirectAttributes.addFlashAttribute("success", "Password changed successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error changing password: " + e.getMessage());
        }
        
        return "redirect:/user/settings";
    }
    
    // ========== DELETE ACCOUNT ==========
    @PostMapping("/settings/delete-account")
    @PreAuthorize("isAuthenticated()")
    public String deleteAccount(@RequestParam("confirmationText") String confirmationText,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(principal.getName());
            
            if (!"DELETE MY ACCOUNT".equals(confirmationText.trim())) {
                redirectAttributes.addFlashAttribute("error", "Confirmation text is incorrect");
                return "redirect:/user/settings";
            }
            
            // Call service to delete account
            userService.deleteUser(user.getId());
            
            redirectAttributes.addFlashAttribute("success", "Your account has been deleted successfully.");
            return "redirect:/logout"; // Redirect to logout after deletion
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting account: " + e.getMessage());
            return "redirect:/user/settings";
        }
    }
    
    // ========== DEBUG ENDPOINTS ==========

    /**
     * Debug endpoint to see what data is being sent to the dashboard
     * Access: /user/dashboard-debug after login
     */
    @GetMapping("/dashboard-debug")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public String debugDashboardData(Principal principal) {
        try {
            User user = userService.findByUsername(principal.getName());
            
            // Get what's being sent to dashboard
            List<Booking> recentBookings = bookingService.getRecentBookings(user, 5);
            DashboardStatisticsWithSpent stats = bookingService.getUserDashboardStatsWithSpent(user);
            
            StringBuilder debug = new StringBuilder();
            debug.append("=== DASHBOARD DATA SENT TO TEMPLATE ===\n\n");
            
            debug.append("User: ").append(user.getUsername()).append(" (ID: ").append(user.getId()).append(")\n");
            debug.append("Total Bookings: ").append(stats.getTotalBookings()).append("\n");
            debug.append("Upcoming: ").append(stats.getUpcomingBookings()).append("\n");
            debug.append("Completed: ").append(stats.getCompletedBookings()).append("\n");
            debug.append("Total Spent: $").append(stats.getTotalSpent()).append("\n\n");
            
            debug.append("Recent Bookings (").append(recentBookings.size()).append("):\n");
            if (recentBookings.isEmpty()) {
                debug.append("  NO BOOKINGS FOUND IN RECENT LIST\n");
            } else {
                for (Booking b : recentBookings) {
                    debug.append("  - Ref: ").append(b.getBookingReference())
                         .append("\n    Status: ").append(b.getStatus())
                         .append("\n    Room: ").append(b.getRoom() != null ? b.getRoom().getRoomType() : "N/A")
                         .append("\n    Check-in: ").append(b.getCheckInDate())
                         .append("\n    Check-out: ").append(b.getCheckOutDate())
                         .append("\n    Guests: ").append(b.getNumberOfGuests())
                         .append("\n    Created: ").append(b.getCreatedAt())
                         .append("\n    Formatted: ").append(b.getFormattedCheckInDate())
                         .append("\n");
                }
            }
            
            return debug.toString().replace("\n", "<br>");
            
        } catch (Exception e) {
            return "Error: " + e.getMessage() + "<br><br>" + 
                   "Stack trace: " + Arrays.toString(e.getStackTrace()).replace(",", "<br>");
        }
    }

    /**
     * Debug endpoint to test booking service directly
     */
    @GetMapping("/test-booking-service")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public String testBookingService(Principal principal) {
        try {
            User user = userService.findByUsername(principal.getName());
            
            // Test getUserBookings
            List<Booking> allBookings = bookingService.getUserBookings(user);
            
            // Test getRecentBookings
            List<Booking> recent = bookingService.getRecentBookings(user, 10);
            
            // Test stats
            DashboardStatisticsWithSpent stats = bookingService.getUserDashboardStatsWithSpent(user);
            
            StringBuilder result = new StringBuilder();
            result.append("=== BOOKING SERVICE TEST ===\n\n");
            
            result.append("getUserBookings() returned: ").append(allBookings.size()).append(" bookings\n");
            for (Booking b : allBookings) {
                result.append("  - ").append(b.getBookingReference()).append("\n");
            }
            
            result.append("\ngetRecentBookings(10) returned: ").append(recent.size()).append(" bookings\n");
            for (Booking b : recent) {
                result.append("  - ").append(b.getBookingReference())
                      .append(" (Created: ").append(b.getCreatedAt()).append(")\n");
            }
            
            result.append("\nStats:\n");
            result.append("  Total: ").append(stats.getTotalBookings()).append("\n");
            result.append("  Upcoming: ").append(stats.getUpcomingBookings()).append("\n");
            result.append("  Completed: ").append(stats.getCompletedBookings()).append("\n");
            result.append("  Total Spent: $").append(stats.getTotalSpent()).append("\n");
            
            return result.toString().replace("\n", "<br>");
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}