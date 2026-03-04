package com.example.hotelbooking.controller;

import com.example.hotelbooking.model.Booking;
import com.example.hotelbooking.model.Room;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.RoomRepository;
import com.example.hotelbooking.repository.UserRepository;
import com.example.hotelbooking.service.BookingService;
import com.example.hotelbooking.service.BookingService.DashboardStatisticsWithSpent;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    
    // ========== BOOKING FLOW ==========
    
    @GetMapping("/book-now/{roomId}")
    @PreAuthorize("hasRole('USER')")
    public String showBookingForm(@PathVariable("roomId") Long roomId,
                                  Model model,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        
        try {
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
            
            if (room.getAvailable() == null || !room.getAvailable()) {
                redirectAttributes.addFlashAttribute("error", "This room is currently unavailable");
                return "redirect:/rooms";
            }
            
            LocalDate defaultCheckIn = LocalDate.now().plusDays(1);
            LocalDate defaultCheckOut = defaultCheckIn.plusDays(2);
            
            long defaultNights = ChronoUnit.DAYS.between(defaultCheckIn, defaultCheckOut);
            BigDecimal defaultTotal = room.getPricePerNight().multiply(BigDecimal.valueOf(defaultNights));
            
            model.addAttribute("room", room);
            model.addAttribute("user", user);
            model.addAttribute("defaultCheckIn", defaultCheckIn);
            model.addAttribute("defaultCheckOut", defaultCheckOut);
            model.addAttribute("defaultNights", defaultNights);
            model.addAttribute("defaultTotal", defaultTotal);
            model.addAttribute("today", LocalDate.now());
            model.addAttribute("maxDate", LocalDate.now().plusMonths(3));
            model.addAttribute("isAuthenticated", true);
            model.addAttribute("username", user.getUsername());
            model.addAttribute("isUser", true);
            model.addAttribute("canBook", true);
            
            return "user/booking-form";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/rooms";
        }
    }
    
    @PostMapping("/create")
    @PreAuthorize("hasRole('USER')")
    public String submitBooking(
            @RequestParam("roomId") Long roomId,
            @RequestParam("checkInDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam("checkOutDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
            @RequestParam("numberOfGuests") Integer numberOfGuests,
            @RequestParam(value = "specialRequests", required = false) String specialRequests,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        try {
            System.out.println("========== SUBMIT BOOKING START ==========");
            
            if (checkInDate.isBefore(LocalDate.now())) {
                redirectAttributes.addFlashAttribute("error", "Check-in date cannot be in the past");
                return "redirect:/bookings/book-now/" + roomId;
            }
            
            if (checkOutDate.isBefore(checkInDate.plusDays(1))) {
                redirectAttributes.addFlashAttribute("error", "Check-out date must be at least 1 day after check-in");
                return "redirect:/bookings/book-now/" + roomId;
            }
            
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
            
            boolean available = bookingService.isRoomAvailable(roomId, checkInDate, checkOutDate);
            
            if (!available) {
                redirectAttributes.addFlashAttribute("error", "Room is not available for the selected dates");
                return "redirect:/bookings/book-now/" + roomId;
            }
            
            Booking booking = bookingService.createBooking(
                user, roomId, checkInDate, checkOutDate, numberOfGuests, specialRequests
            );
            
            if (booking != null && booking.getId() != null) {
                redirectAttributes.addFlashAttribute("success", 
                    "Booking confirmed successfully! Your booking reference is: " + booking.getBookingReference());
                return "redirect:/bookings/confirmation/" + booking.getId();
            } else {
                throw new RuntimeException("Booking creation failed");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Booking failed: " + e.getMessage());
            return "redirect:/bookings/book-now/" + roomId;
        }
    }
    
    @GetMapping("/confirmation/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public String showConfirmation(@PathVariable("bookingId") Long bookingId,
                                  Model model,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        
        try {
            Booking booking = bookingService.getBookingById(bookingId);
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!booking.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "Access denied");
                return "redirect:/user/dashboard";
            }
            
            booking.initializeTransientFields();
            long totalNights = booking.getNights() != null ? booking.getNights() : 
                ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
            
            model.addAttribute("booking", booking);
            model.addAttribute("totalNights", totalNights);
            model.addAttribute("username", user.getUsername());
            model.addAttribute("isAuthenticated", true);
            
            return "user/booking-confirmation";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/user/dashboard";
        }
    }
    
    // ========== USER BOOKINGS MANAGEMENT ==========
    
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public String myBookings(@RequestParam(value = "status", required = false) String statusFilter,
                            Model model, 
                            Principal principal) {
        
        try {
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<Booking> bookings = bookingService.getUserBookings(user);
            
            if (statusFilter != null && !statusFilter.isEmpty()) {
                String statusUpper = statusFilter.toUpperCase();
                List<Booking> filteredBookings = new ArrayList<>();
                
                for (Booking booking : bookings) {
                    if ("UPCOMING".equals(statusUpper)) {
                        if (booking.isUpcoming() && "CONFIRMED".equals(booking.getStatus())) {
                            filteredBookings.add(booking);
                        }
                    } else if ("COMPLETED".equals(statusUpper)) {
                        if (booking.isCompleted()) {
                            filteredBookings.add(booking);
                        }
                    } else {
                        if (statusUpper.equals(booking.getStatus())) {
                            filteredBookings.add(booking);
                        }
                    }
                }
                bookings = filteredBookings;
                model.addAttribute("statusFilter", statusFilter);
            }
            
            DashboardStatisticsWithSpent stats = bookingService.getUserDashboardStatsWithSpent(user);
            
            model.addAttribute("bookings", bookings);
            model.addAttribute("user", user);
            model.addAttribute("stats", stats);
            model.addAttribute("isAuthenticated", true);
            model.addAttribute("userFullName", user.getFullName() != null ? user.getFullName() : user.getUsername());
            
            return "user/my-bookings";
            
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error loading bookings: " + e.getMessage());
            model.addAttribute("bookings", new ArrayList<>());
            return "user/my-bookings";
        }
    }
    
    @GetMapping("/view/{id}")
    @PreAuthorize("isAuthenticated()")
    public String viewBooking(@PathVariable("id") Long id,
                            Model model,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {
        
        try {
            Booking booking = bookingService.getBookingById(id);
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!booking.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "You don't have permission to view this booking");
                return "redirect:/bookings/my";
            }
            
            booking.initializeTransientFields();
            long nights = booking.getNights() != null ? booking.getNights() : 
                ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
            
            BigDecimal tax = booking.getTotalPrice().multiply(new BigDecimal("0.10"));
            BigDecimal totalWithTax = booking.getTotalPrice().add(tax);
            
            model.addAttribute("booking", booking);
            model.addAttribute("nights", nights);
            model.addAttribute("tax", tax);
            model.addAttribute("totalWithTax", totalWithTax);
            model.addAttribute("user", user);
            model.addAttribute("isAuthenticated", true);
            model.addAttribute("userFullName", user.getFullName() != null ? user.getFullName() : user.getUsername());
            
            return "user/booking-details";
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error loading booking: " + e.getMessage());
            return "redirect:/bookings/my";
        }
    }
    
    @PostMapping("/cancel/{id}")
    @PreAuthorize("isAuthenticated()")
    public String cancelBooking(@PathVariable("id") Long id,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        
        try {
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Booking booking = bookingService.getBookingById(id);
            
            if (!booking.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "You cannot cancel this booking");
                return "redirect:/bookings/my";
            }
            
            if (booking.getCheckInDate().isBefore(LocalDate.now())) {
                redirectAttributes.addFlashAttribute("error", "Cannot cancel past bookings");
                return "redirect:/bookings/my";
            }
            
            if ("CANCELLED".equals(booking.getStatus())) {
                redirectAttributes.addFlashAttribute("error", "Booking is already cancelled");
                return "redirect:/bookings/my";
            }
            
            bookingService.cancelBooking(id);
            redirectAttributes.addFlashAttribute("success", "Booking cancelled successfully");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error cancelling booking: " + e.getMessage());
        }
        
        return "redirect:/bookings/my";
    }
    
    // ========== AVAILABILITY CHECK ==========
    
    @GetMapping("/check-availability")
    @ResponseBody
    public Map<String, Object> checkAvailability(@RequestParam("roomId") Long roomId,
                                               @RequestParam("checkIn") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                                               @RequestParam("checkOut") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        
        try {
            Room room = roomRepository.findById(roomId).orElse(null);
            if (room == null || room.getAvailable() == null || !room.getAvailable()) {
                return Map.of("available", false, "message", "Room is not available");
            }
            
            boolean isAvailable = bookingService.isRoomAvailable(roomId, checkIn, checkOut);
            
            if (isAvailable) {
                BigDecimal price = bookingService.calculatePrice(roomId, checkIn, checkOut);
                long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
                
                return Map.of(
                    "available", true,
                    "message", "Room is available",
                    "price", price,
                    "nights", nights
                );
            } else {
                return Map.of("available", false, "message", "Room is already booked for selected dates");
            }
            
        } catch (Exception e) {
            return Map.of("available", false, "message", "Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/calculate-price")
    @ResponseBody
    public Map<String, Object> calculatePrice(@RequestParam("roomId") Long roomId,
                                            @RequestParam("checkIn") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                                            @RequestParam("checkOut") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        
        try {
            BigDecimal price = bookingService.calculatePrice(roomId, checkIn, checkOut);
            long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
            
            return Map.of(
                "success", true,
                "price", price,
                "nights", nights,
                "formattedPrice", "$" + price.setScale(2).toString()
            );
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error: " + e.getMessage());
        }
    }
    
    // ========== DASHBOARD METHODS ==========
    
    @GetMapping("/dashboard-stats")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public DashboardStatisticsWithSpent getDashboardStats(Principal principal) {
        try {
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            return bookingService.getUserDashboardStatsWithSpent(user);
        } catch (Exception e) {
            return new DashboardStatisticsWithSpent(0L, 0L, 0L, 0L, BigDecimal.ZERO);
        }
    }
    
    @GetMapping("/recent-bookings")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public List<Booking> getRecentBookings(Principal principal) {
        try {
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<Booking> recent = bookingService.getRecentBookings(user, 5);
            recent.forEach(Booking::initializeTransientFields);
            return recent;
            
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    // ========== TEST ENDPOINT ==========
    
    @GetMapping("/test-save")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public String testSaveBooking(Principal principal) {
        try {
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Room room = roomRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No rooms found"));
            
            Booking testBooking = new Booking();
            testBooking.setUser(user);
            testBooking.setRoom(room);
            testBooking.setCheckInDate(LocalDate.now().plusDays(10));
            testBooking.setCheckOutDate(LocalDate.now().plusDays(12));
            testBooking.setNumberOfGuests(2);
            testBooking.setTotalPrice(room.getPricePerNight().multiply(BigDecimal.valueOf(2)));
            testBooking.setStatus("CONFIRMED");
            testBooking.setBookingReference("TEST" + System.currentTimeMillis());
            testBooking.setCreatedAt(LocalDateTime.now());
            testBooking.setUpdatedAt(LocalDateTime.now());
            
            Booking saved = bookingRepository.save(testBooking);
            bookingRepository.flush();
            
            return "SUCCESS! Test booking saved with ID: " + saved.getId();
            
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
    @GetMapping("/test-simple-booking")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public String testSimpleBooking(Principal principal) {
        try {
            System.out.println("========== SIMPLE TEST BOOKING ==========");
            
            // Get user
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            System.out.println("User: " + user.getUsername() + " (ID: " + user.getId() + ")");
            
            // Get first room
            Room room = roomRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No rooms found"));
            System.out.println("Room: " + room.getRoomNumber() + " (ID: " + room.getId() + ")");
            
            // Create booking manually (not using service)
            Booking booking = new Booking();
            booking.setUser(user);
            booking.setRoom(room);
            booking.setCheckInDate(LocalDate.now().plusDays(5));
            booking.setCheckOutDate(LocalDate.now().plusDays(7));
            booking.setNumberOfGuests(2);
            booking.setTotalPrice(new BigDecimal("299.99"));
            booking.setStatus("CONFIRMED");
            booking.setBookingReference("SIMPLE-" + System.currentTimeMillis());
            booking.setCreatedAt(LocalDateTime.now());
            booking.setUpdatedAt(LocalDateTime.now());
            
            System.out.println("Saving simple test booking...");
            Booking saved = bookingRepository.save(booking);
            bookingRepository.flush();
            
            System.out.println("Saved with ID: " + saved.getId());
            System.out.println("==========================================");
            
            return "SUCCESS! Simple booking saved with ID: " + saved.getId();
            
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }
    @GetMapping("/check-bookings")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public String checkBookings(Principal principal) {
        StringBuilder result = new StringBuilder();
        try {
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            result.append("User: ").append(user.getUsername()).append(" (ID: ").append(user.getId()).append(")\n\n");
            
            // Method 1: Direct SQL via repository
            List<Booking> allBookings = bookingRepository.findAll();
            result.append("ALL BOOKINGS IN DATABASE (").append(allBookings.size()).append("):\n");
            for (Booking b : allBookings) {
                result.append("  ID: ").append(b.getId())
                      .append(", Ref: ").append(b.getBookingReference())
                      .append(", UserID: ").append(b.getUser() != null ? b.getUser().getId() : "null")
                      .append(", Status: ").append(b.getStatus())
                      .append(", Created: ").append(b.getCreatedAt())
                      .append("\n");
            }
            
            // Method 2: Find by user
            List<Booking> userBookings = bookingRepository.findByUser(user);
            result.append("\nUSER BOOKINGS (findByUser): ").append(userBookings.size()).append("\n");
            for (Booking b : userBookings) {
                result.append("  ").append(b.getBookingReference()).append("\n");
            }
            
            // Method 3: Find by user with room
            List<Booking> userBookingsWithRoom = bookingRepository.findByUserWithRoom(user);
            result.append("\nUSER BOOKINGS (findByUserWithRoom): ").append(userBookingsWithRoom.size()).append("\n");
            for (Booking b : userBookingsWithRoom) {
                result.append("  ").append(b.getBookingReference()).append("\n");
            }
            
        } catch (Exception e) {
            result.append("ERROR: ").append(e.getMessage());
        }
        
        return result.toString().replace("\n", "<br>");
    }
}