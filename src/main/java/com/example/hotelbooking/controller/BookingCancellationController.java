package com.example.hotelbooking.controller;

import com.example.hotelbooking.model.Booking;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.service.BookingService;
import com.example.hotelbooking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/bookings")
public class BookingCancellationController {

    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable("id") Long bookingId, 
                               RedirectAttributes redirectAttributes) {
        try {
            // Get current authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);
            
            // Get the booking
            Booking booking = bookingService.getBookingById(bookingId);
            
            // Verify that this booking belongs to the current user
            if (!booking.getUser().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", 
                    "You don't have permission to cancel this booking");
                return "redirect:/bookings/my";
            }
            
            // Cancel the booking
            bookingService.cancelBooking(bookingId);
            
            redirectAttributes.addFlashAttribute("success", 
                "Booking #" + booking.getBookingReference() + " has been cancelled successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Failed to cancel booking: " + e.getMessage());
        }
        
        return "redirect:/bookings/my";
    }
}