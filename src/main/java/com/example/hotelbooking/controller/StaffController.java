package com.example.hotelbooking.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/staff")
@PreAuthorize("hasRole('STAFF')")
public class StaffController {
    
    @GetMapping("/dashboard")
    public String staffDashboard(Model model) {
        // Add statistics
        model.addAttribute("totalBookings", 15);
        model.addAttribute("availableRooms", 8);
        model.addAttribute("todayCheckins", 3);
        model.addAttribute("todayRevenue", "$1,250");
        return "staff/dashboard";
    }
    
    @GetMapping("/rooms")
    public String viewRoomStatus() {
        return "staff/rooms";
    }
    
    @GetMapping("/bookings")
    public String manageBookings() {
        return "staff/bookings";
    }
    
    @GetMapping("/bookings/{id}")
    public String viewBooking(@PathVariable Long id, Model model) {
        model.addAttribute("bookingId", id);
        return "staff/booking-details";
    }
    
    @PostMapping("/bookings/{id}/update-status")
    public String updateBookingStatus(@PathVariable Long id, 
                                     @RequestParam String status) {
        // Update booking status logic
        return "redirect:/staff/bookings/" + id;
    }
    
    @GetMapping("/checkin")
    public String checkinPage() {
        return "staff/checkin";
    }
    
    @PostMapping("/checkin")
    public String processCheckin(@RequestParam Long bookingId) {
        // Process check-in logic
        return "redirect:/staff/dashboard";
    }
    
    @GetMapping("/checkout")
    public String checkoutPage() {
        return "staff/checkout";
    }
    
    @PostMapping("/checkout")
    public String processCheckout(@RequestParam Long bookingId) {
        // Process check-out logic
        return "redirect:/staff/dashboard";
    }
    
    @GetMapping("/create-booking")
    public String createBookingForm() {
        return "staff/create-booking";
    }
    
    @PostMapping("/create-booking")
    public String createBooking(@RequestParam Long guestId,
                               @RequestParam Long roomId,
                               @RequestParam String checkInDate,
                               @RequestParam String checkOutDate) {
        // Create booking logic
        return "redirect:/staff/dashboard";
    }
}