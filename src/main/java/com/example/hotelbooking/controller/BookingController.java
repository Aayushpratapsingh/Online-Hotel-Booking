package com.example.hotelbooking.controller;

import com.example.hotelbooking.model.Booking;
import com.example.hotelbooking.model.Room;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.RoomRepository;
import com.example.hotelbooking.repository.UserRepository;
import com.example.hotelbooking.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/bookings")
public class BookingController {
    
    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    
    public BookingController(BookingService bookingService, 
                           UserRepository userRepository,
                           RoomRepository roomRepository,
                           BookingRepository bookingRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }
    
    /**
     * Show booking form for a specific room
     */
    @GetMapping("/new")
    public String showBookingForm(@RequestParam Long roomId, 
                                  Model model, 
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to book a room");
            return "redirect:/login";
        }
        
        try {
            // Get room details
            Optional<Room> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Room not found");
                return "redirect:/rooms";
            }
            
            Room room = roomOpt.get();
            if (!room.isAvailable()) {
                redirectAttributes.addFlashAttribute("error", "This room is not available");
                return "redirect:/rooms";
            }
            
            // Set default dates (check-in tomorrow, check-out 3 days later)
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            LocalDate checkout = tomorrow.plusDays(3);
            
            model.addAttribute("room", room);
            model.addAttribute("roomId", roomId);
            model.addAttribute("defaultCheckIn", tomorrow);
            model.addAttribute("defaultCheckOut", checkout);
            model.addAttribute("minDate", LocalDate.now().plusDays(1));
            
            return "booking-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error loading booking form: " + e.getMessage());
            return "redirect:/rooms";
        }
    }
    
    /**
     * Handle booking creation
     */
    @PostMapping("/create")
    public String createBooking(
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam Integer guests,
            @RequestParam(required = false) String specialRequests,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to book a room");
            return "redirect:/login";
        }
        
        try {
            // Validate dates
            if (checkIn.isBefore(LocalDate.now().plusDays(1))) {
                redirectAttributes.addFlashAttribute("error", "Check-in date must be at least 1 day in advance");
                return "redirect:/bookings/new?roomId=" + roomId;
            }
            
            if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
                redirectAttributes.addFlashAttribute("error", "Check-out date must be after check-in date");
                return "redirect:/bookings/new?roomId=" + roomId;
            }
            
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Booking booking = bookingService.createBooking(
                user, roomId, checkIn, checkOut, guests, specialRequests
            );
            
            redirectAttributes.addFlashAttribute("success", "Booking created successfully!");
            return "redirect:/bookings/confirmation/" + booking.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Booking failed: " + e.getMessage());
            return "redirect:/bookings/new?roomId=" + roomId;
        }
    }
    
    /**
     * Show booking confirmation page
     */
    @GetMapping("/confirmation/{id}")
    public String bookingConfirmation(@PathVariable Long id, 
                                     Model model, 
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to view booking confirmation");
            return "redirect:/login";
        }
        
        try {
            // Fetch the booking
            Optional<Booking> bookingOpt = bookingRepository.findById(id);
            if (bookingOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/bookings/my";
            }
            
            Booking booking = bookingOpt.get();
            
            // Verify this booking belongs to the logged-in user
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!booking.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "You don't have permission to view this booking");
                return "redirect:/bookings/my";
            }
            
            // Calculate total nights and price if not already set
            if (booking.getTotalPrice() == null || booking.getTotalPrice().equals(BigDecimal.ZERO)) {
                long nights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
                BigDecimal totalPrice = booking.getRoom().getPricePerNight().multiply(BigDecimal.valueOf(nights));
                booking.setTotalPrice(totalPrice);
            }
            
            model.addAttribute("booking", booking);
            model.addAttribute("totalNights", ChronoUnit.DAYS.between(
                booking.getCheckInDate(), booking.getCheckOutDate()));
            
            return "booking-confirmation";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading booking confirmation: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Show user's bookings
     */
    @GetMapping("/my")
    public String myBookings(Model model, 
                            Principal principal,
                            @RequestParam(value = "status", required = false) String statusFilter,
                            RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to view your bookings");
            return "redirect:/login";
        }
        
        try {
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<Booking> bookings;
            if (statusFilter != null && !statusFilter.isEmpty()) {
                bookings = bookingRepository.findByUserIdAndStatus(user.getId(), statusFilter);
            } else {
                bookings = bookingRepository.findByUserId(user.getId());
            }
            
            // Calculate total nights for each booking
            bookings.forEach(booking -> {
                if (booking.getTotalPrice() == null || booking.getTotalPrice().equals(BigDecimal.ZERO)) {
                    long nights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
                    BigDecimal totalPrice = booking.getRoom().getPricePerNight().multiply(BigDecimal.valueOf(nights));
                    booking.setTotalPrice(totalPrice);
                }
            });
            
            model.addAttribute("bookings", bookings);
            model.addAttribute("user", user);
            model.addAttribute("activeFilter", statusFilter);
            
            // Count bookings by status for filter buttons
            model.addAttribute("totalBookings", bookingRepository.countByUserId(user.getId()));
            model.addAttribute("confirmedBookings", bookingRepository.countByUserIdAndStatus(user.getId(), "CONFIRMED"));
            model.addAttribute("cancelledBookings", bookingRepository.countByUserIdAndStatus(user.getId(), "CANCELLED"));
            model.addAttribute("completedBookings", bookingRepository.countByUserIdAndStatus(user.getId(), "COMPLETED"));
            
            return "my-bookings";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading bookings: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Cancel a booking
     */
    @PostMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable Long id,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to cancel a booking");
            return "redirect:/login";
        }
        
        try {
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Verify booking belongs to user
            Optional<Booking> bookingOpt = bookingRepository.findById(id);
            if (bookingOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/bookings/my";
            }
            
            Booking booking = bookingOpt.get();
            if (!booking.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "You cannot cancel this booking");
                return "redirect:/bookings/my";
            }
            
            // Check if booking can be cancelled (not already cancelled or checked in)
            if ("CANCELLED".equals(booking.getStatus())) {
                redirectAttributes.addFlashAttribute("error", "Booking is already cancelled");
                return "redirect:/bookings/my";
            }
            
            if ("CHECKED_IN".equals(booking.getStatus())) {
                redirectAttributes.addFlashAttribute("error", "Cannot cancel booking after check-in");
                return "redirect:/bookings/my";
            }
            
            bookingService.cancelBooking(id);
            redirectAttributes.addFlashAttribute("success", "Booking cancelled successfully");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error cancelling booking: " + e.getMessage());
        }
        
        return "redirect:/bookings/my";
    }
    
    /**
     * View booking details
     */
    @GetMapping("/view/{id}")
    public String viewBooking(@PathVariable Long id,
                            Model model,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to view booking details");
            return "redirect:/login";
        }
        
        try {
            Optional<Booking> bookingOpt = bookingRepository.findById(id);
            if (bookingOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/bookings/my";
            }
            
            Booking booking = bookingOpt.get();
            
            // Verify booking belongs to user
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!booking.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "You don't have permission to view this booking");
                return "redirect:/bookings/my";
            }
            
            // Calculate details
            long nights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
            BigDecimal totalPrice = booking.getRoom().getPricePerNight().multiply(BigDecimal.valueOf(nights));
            
            model.addAttribute("booking", booking);
            model.addAttribute("totalNights", nights);
            model.addAttribute("totalPrice", totalPrice);
            model.addAttribute("canCancel", 
                !"CANCELLED".equals(booking.getStatus()) && 
                !"CHECKED_IN".equals(booking.getStatus()) &&
                booking.getCheckInDate().isAfter(LocalDate.now()));
            
            return "booking-details";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading booking: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Check room availability for dates
     */
    @GetMapping("/check-availability")
    @ResponseBody
    public String checkAvailability(@RequestParam Long roomId,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        try {
            boolean isAvailable = bookingService.isRoomAvailable(roomId, checkIn, checkOut);
            return isAvailable ? "AVAILABLE" : "NOT_AVAILABLE";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
    
    /**
     * Calculate booking price
     */
    @GetMapping("/calculate-price")
    @ResponseBody
    public String calculatePrice(@RequestParam Long roomId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        try {
            Optional<Room> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isEmpty()) {
                return "ERROR: Room not found";
            }
            
            Room room = roomOpt.get();
            long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
            BigDecimal totalPrice = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
            
            return String.format("%.2f|%d", totalPrice, nights);
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
    
    /**
     * Modify booking (show form)
     */
    @GetMapping("/modify/{id}")
    public String showModifyForm(@PathVariable Long id,
                                Model model,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to modify booking");
            return "redirect:/login";
        }
        
        try {
            Optional<Booking> bookingOpt = bookingRepository.findById(id);
            if (bookingOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/bookings/my";
            }
            
            Booking booking = bookingOpt.get();
            
            // Verify booking belongs to user
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!booking.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "You cannot modify this booking");
                return "redirect:/bookings/my";
            }
            
            // Check if booking can be modified
            if ("CANCELLED".equals(booking.getStatus())) {
                redirectAttributes.addFlashAttribute("error", "Cannot modify cancelled booking");
                return "redirect:/bookings/my";
            }
            
            if ("CHECKED_IN".equals(booking.getStatus())) {
                redirectAttributes.addFlashAttribute("error", "Cannot modify booking after check-in");
                return "redirect:/bookings/my";
            }
            
            model.addAttribute("booking", booking);
            model.addAttribute("room", booking.getRoom());
            model.addAttribute("minDate", LocalDate.now().plusDays(1));
            
            return "modify-booking";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading modify form: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Update booking
     */
    @PostMapping("/update/{id}")
    public String updateBooking(@PathVariable Long id,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newCheckIn,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newCheckOut,
                               @RequestParam Integer newGuests,
                               @RequestParam(required = false) String specialRequests,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to modify booking");
            return "redirect:/login";
        }
        
        try {
            // Validate dates
            if (newCheckIn.isBefore(LocalDate.now().plusDays(1))) {
                redirectAttributes.addFlashAttribute("error", "Check-in date must be at least 1 day in advance");
                return "redirect:/bookings/modify/" + id;
            }
            
            if (newCheckOut.isBefore(newCheckIn) || newCheckOut.isEqual(newCheckIn)) {
                redirectAttributes.addFlashAttribute("error", "Check-out date must be after check-in date");
                return "redirect:/bookings/modify/" + id;
            }
            
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            bookingService.updateBooking(id, newCheckIn, newCheckOut, newGuests, specialRequests, user.getId());
            
            redirectAttributes.addFlashAttribute("success", "Booking updated successfully!");
            return "redirect:/bookings/view/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Update failed: " + e.getMessage());
            return "redirect:/bookings/modify/" + id;
        }
    }
}