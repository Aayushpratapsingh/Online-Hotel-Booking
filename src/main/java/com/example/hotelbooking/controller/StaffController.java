package com.example.hotelbooking.controller;

import com.example.hotelbooking.model.Booking;
import com.example.hotelbooking.model.Room;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.RoomRepository;
import com.example.hotelbooking.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/staff")
@PreAuthorize("hasRole('STAFF')")
public class StaffController {
    
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    
    public StaffController(BookingRepository bookingRepository, 
                          RoomRepository roomRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }
    
    // ========== DASHBOARD ==========
    
    @GetMapping("/dashboard")
    public String staffDashboard(Model model, Authentication authentication) {
        
        // Get staff info
        String username = authentication.getName();
        User staff = userRepository.findByUsername(username).orElse(null);
        
        // Get all data
        List<Booking> allBookings = bookingRepository.findAll();
        List<Room> allRooms = roomRepository.findAll();
        LocalDate today = LocalDate.now();
        
        // ========== STATISTICS ==========
        
        // Today's bookings (created today)
        long todaysBookings = allBookings.stream()
            .filter(b -> b.getCreatedAt() != null && 
                         b.getCreatedAt().toLocalDate().equals(today))
            .count();
        
        // Active bookings (checked-in)
        long activeBookings = allBookings.stream()
            .filter(b -> "CHECKED_IN".equals(b.getStatus()))
            .count();
        
        // Room statistics
        long totalRooms = allRooms.size();
        long availableRooms = allRooms.stream()
            .filter(r -> r.getAvailable() != null && r.getAvailable())
            .count();
        long occupiedRooms = allRooms.stream()
            .filter(r -> r.getAvailable() != null && !r.getAvailable() && 
                        (r.getStatus() == null || "occupied".equals(r.getStatus())))
            .count();
        
        // Maintenance and cleaning counts
        long maintenanceRooms = allRooms.stream()
            .filter(r -> "maintenance".equals(r.getStatus()))
            .count();
        long cleaningRooms = allRooms.stream()
            .filter(r -> "cleaning".equals(r.getStatus()))
            .count();
        
        // Occupancy rate
        int occupancyRate = totalRooms > 0 ? 
            (int) Math.round((occupiedRooms * 100.0) / totalRooms) : 0;
        
        // ========== CHECK-IN / CHECK-OUT DATA ==========
        
        // Today's check-ins (CONFIRMED bookings checking in today)
        List<Booking> todaysCheckins = allBookings.stream()
            .filter(b -> "CONFIRMED".equals(b.getStatus()) && 
                         b.getCheckInDate() != null && 
                         b.getCheckInDate().equals(today))
            .limit(5)
            .collect(Collectors.toList());
        
        // Today's check-outs (CHECKED_IN bookings checking out today)
        List<Booking> todaysCheckouts = allBookings.stream()
            .filter(b -> "CHECKED_IN".equals(b.getStatus()) && 
                         b.getCheckOutDate() != null && 
                         b.getCheckOutDate().equals(today))
            .limit(5)
            .collect(Collectors.toList());
        
        // Currently occupied rooms (all CHECKED_IN bookings)
        List<Booking> occupiedRoomsList = allBookings.stream()
            .filter(b -> "CHECKED_IN".equals(b.getStatus()))
            .collect(Collectors.toList());
        
        // ========== RECENT BOOKINGS ==========
        
        List<Booking> recentBookings = allBookings.stream()
            .sorted((b1, b2) -> {
                if (b1.getCreatedAt() == null) return 1;
                if (b2.getCreatedAt() == null) return -1;
                return b2.getCreatedAt().compareTo(b1.getCreatedAt());
            })
            .limit(10)
            .collect(Collectors.toList());
        
        // Initialize transient fields for each booking
        todaysCheckins.forEach(Booking::initializeTransientFields);
        todaysCheckouts.forEach(Booking::initializeTransientFields);
        occupiedRoomsList.forEach(Booking::initializeTransientFields);
        recentBookings.forEach(Booking::initializeTransientFields);
        
        // ========== ADD ATTRIBUTES TO MODEL ==========
        
        model.addAttribute("staffName", staff != null ? staff.getFullName() : username);
        model.addAttribute("todaysBookings", todaysBookings);
        model.addAttribute("activeBookings", activeBookings);
        model.addAttribute("totalRooms", totalRooms);
        model.addAttribute("availableRooms", availableRooms);
        model.addAttribute("occupiedRooms", occupiedRooms);
        model.addAttribute("maintenanceRooms", maintenanceRooms);
        model.addAttribute("cleaningRooms", cleaningRooms);
        model.addAttribute("occupancyRate", occupancyRate);
        
        // Check-in/out attributes
        model.addAttribute("todaysCheckins", todaysCheckins);
        model.addAttribute("todaysCheckouts", todaysCheckouts);
        model.addAttribute("todaysCheckinsCount", todaysCheckins.size());
        model.addAttribute("todaysCheckoutsCount", todaysCheckouts.size());
        model.addAttribute("occupiedRoomsList", occupiedRoomsList);
        
        // Recent bookings
        model.addAttribute("recentBookings", recentBookings);
        
        // Current date
        model.addAttribute("currentDate", today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        
        return "staff/dashboard";
    }
    
    // ========== ROOMS MONITORING ==========
    
    @GetMapping("/rooms")
    public String monitorRooms(Model model, Authentication authentication) {
        // Get staff info
        String username = authentication.getName();
        User staff = userRepository.findByUsername(username).orElse(null);
        
        // Get all rooms
        List<Room> rooms = roomRepository.findAll();
        
        // Calculate counts
        long availableCount = rooms.stream()
            .filter(r -> r.getAvailable() != null && r.getAvailable())
            .count();
        long occupiedCount = rooms.stream()
            .filter(r -> r.getAvailable() != null && !r.getAvailable() && 
                        (r.getStatus() == null || "occupied".equals(r.getStatus())))
            .count();
        long maintenanceCount = rooms.stream()
            .filter(r -> "maintenance".equals(r.getStatus()))
            .count();
        long cleaningCount = rooms.stream()
            .filter(r -> "cleaning".equals(r.getStatus()))
            .count();
        
        model.addAttribute("staffName", staff != null ? staff.getFullName() : username);
        model.addAttribute("rooms", rooms);
        model.addAttribute("availableCount", availableCount);
        model.addAttribute("occupiedCount", occupiedCount);
        model.addAttribute("maintenanceCount", maintenanceCount);
        model.addAttribute("cleaningCount", cleaningCount);
        
        return "staff/rooms";
    }
    
    @PostMapping("/rooms/{id}/update-status")
    public String updateRoomStatus(@PathVariable("id") Long id,
                                  @RequestParam("status") String status,
                                  RedirectAttributes redirectAttributes) {
        try {
            Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));
            
            // Update room status
            if ("available".equals(status)) {
                room.setAvailable(true);
                room.setStatus(null);
            } else if ("occupied".equals(status)) {
                room.setAvailable(false);
                room.setStatus(null);
            } else {
                room.setAvailable(false);
                room.setStatus(status);
            }
            
            roomRepository.save(room);
            
            redirectAttributes.addFlashAttribute("success", "Room " + room.getRoomNumber() + " status updated successfully");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update room: " + e.getMessage());
        }
        
        return "redirect:/staff/rooms";
    }
    
    // ========== BOOKINGS MANAGEMENT ==========
    
    @GetMapping("/bookings")
    public String manageBookings(Model model, Authentication authentication) {
        String username = authentication.getName();
        User staff = userRepository.findByUsername(username).orElse(null);
        
        List<Booking> bookings = bookingRepository.findAll().stream()
            .sorted((b1, b2) -> {
                if (b1.getCheckInDate() == null) return 1;
                if (b2.getCheckInDate() == null) return -1;
                return b2.getCheckInDate().compareTo(b1.getCheckInDate());
            })
            .collect(Collectors.toList());
        
        bookings.forEach(Booking::initializeTransientFields);
        
        model.addAttribute("staffName", staff != null ? staff.getFullName() : username);
        model.addAttribute("bookings", bookings);
        return "staff/bookings";
    }
    
    @GetMapping("/bookings/{id}")
    public String viewBooking(@PathVariable("id") Long id, Model model, 
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            User staff = userRepository.findByUsername(username).orElse(null);
            
            Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            booking.initializeTransientFields();
            
            // Calculate nights
            long nights = booking.getNights() != null ? booking.getNights() : 
                java.time.temporal.ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
            
            model.addAttribute("staffName", staff != null ? staff.getFullName() : username);
            model.addAttribute("booking", booking);
            model.addAttribute("nights", nights);
            
            return "staff/booking-details";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Booking not found");
            return "redirect:/staff/bookings";
        }
    }
    
    // ========== CHECK-IN METHODS ==========
    
    @GetMapping("/checkin")
    public String showCheckinPage(Model model, Authentication authentication) {
        String username = authentication.getName();
        User staff = userRepository.findByUsername(username).orElse(null);
        
        LocalDate today = LocalDate.now();
        
        // Get all CONFIRMED bookings for today
        List<Booking> todaysCheckins = bookingRepository.findAll().stream()
            .filter(b -> "CONFIRMED".equals(b.getStatus()) && 
                         b.getCheckInDate() != null &&
                         b.getCheckInDate().equals(today))
            .collect(Collectors.toList());
        
        // Get all CONFIRMED bookings for future dates
        List<Booking> futureCheckins = bookingRepository.findAll().stream()
            .filter(b -> "CONFIRMED".equals(b.getStatus()) && 
                         b.getCheckInDate() != null &&
                         b.getCheckInDate().isAfter(today))
            .collect(Collectors.toList());
        
        todaysCheckins.forEach(Booking::initializeTransientFields);
        futureCheckins.forEach(Booking::initializeTransientFields);
        
        model.addAttribute("staffName", staff != null ? staff.getFullName() : username);
        model.addAttribute("todaysCheckins", todaysCheckins);
        model.addAttribute("futureCheckins", futureCheckins);
        model.addAttribute("today", today);
        
        return "staff/checkin";
    }
    
    @PostMapping("/booking/{id}/checkin")
    public String quickCheckin(@PathVariable("id") Long id, 
                              RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            if (!"CONFIRMED".equals(booking.getStatus())) {
                throw new RuntimeException("Only confirmed bookings can be checked in");
            }
            
            booking.setStatus("CHECKED_IN");
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            
            // Make room unavailable
            Room room = booking.getRoom();
            room.setAvailable(false);
            room.setStatus(null);
            roomRepository.save(room);
            
            redirectAttributes.addFlashAttribute("success", 
                "Guest checked in successfully. Room " + room.getRoomNumber() + " is now occupied.");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Check-in failed: " + e.getMessage());
        }
        
        return "redirect:/staff/dashboard";
    }
    
    @PostMapping("/checkin/process")
    public String processCheckin(@RequestParam("bookingId") Long bookingId,
                                @RequestParam(value = "notes", required = false) String notes,
                                RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            if (!"CONFIRMED".equals(booking.getStatus())) {
                throw new RuntimeException("Only confirmed bookings can be checked in");
            }
            
            booking.setStatus("CHECKED_IN");
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            
            // Make room unavailable
            Room room = booking.getRoom();
            room.setAvailable(false);
            room.setStatus(null);
            roomRepository.save(room);
            
            redirectAttributes.addFlashAttribute("success", 
                "Guest checked in successfully. Room " + room.getRoomNumber() + " is now occupied.");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Check-in failed: " + e.getMessage());
        }
        
        return "redirect:/staff/checkin";
    }
    
    // ========== CHECK-OUT METHODS ==========
    
    @GetMapping("/checkout")
    public String showCheckoutPage(Model model, Authentication authentication) {
        String username = authentication.getName();
        User staff = userRepository.findByUsername(username).orElse(null);
        
        LocalDate today = LocalDate.now();
        
        // Get all CHECKED_IN bookings (currently occupied)
        List<Booking> occupiedRooms = bookingRepository.findAll().stream()
            .filter(b -> "CHECKED_IN".equals(b.getStatus()))
            .collect(Collectors.toList());
        
        // Get bookings checking out today
        List<Booking> todaysCheckouts = occupiedRooms.stream()
            .filter(b -> b.getCheckOutDate() != null && b.getCheckOutDate().equals(today))
            .collect(Collectors.toList());
        
        occupiedRooms.forEach(Booking::initializeTransientFields);
        todaysCheckouts.forEach(Booking::initializeTransientFields);
        
        model.addAttribute("staffName", staff != null ? staff.getFullName() : username);
        model.addAttribute("occupiedRooms", occupiedRooms);
        model.addAttribute("todaysCheckouts", todaysCheckouts);
        model.addAttribute("today", today);
        
        return "staff/checkout";
    }
    
    @PostMapping("/booking/{id}/checkout")
    public String quickCheckout(@PathVariable("id") Long id, 
                               RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            if (!"CHECKED_IN".equals(booking.getStatus())) {
                throw new RuntimeException("Only checked-in bookings can be checked out");
            }
            
            booking.setStatus("CHECKED_OUT");
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            
            // Make room available again
            Room room = booking.getRoom();
            room.setAvailable(true);
            room.setStatus(null);
            roomRepository.save(room);
            
            redirectAttributes.addFlashAttribute("success", 
                "Guest checked out successfully. Room " + room.getRoomNumber() + " is now available.");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Check-out failed: " + e.getMessage());
        }
        
        return "redirect:/staff/dashboard";
    }
    
    @PostMapping("/checkout/process")
    public String processCheckout(@RequestParam("bookingId") Long bookingId,
                                 @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
                                 @RequestParam(value = "notes", required = false) String notes,
                                 RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            if (!"CHECKED_IN".equals(booking.getStatus())) {
                throw new RuntimeException("Only checked-in bookings can be checked out");
            }
            
            booking.setStatus("CHECKED_OUT");
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            
            // Make room available again
            Room room = booking.getRoom();
            room.setAvailable(true);
            room.setStatus(null);
            roomRepository.save(room);
            
            redirectAttributes.addFlashAttribute("success", 
                "Guest checked out successfully. Room " + room.getRoomNumber() + " is now available.");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Check-out failed: " + e.getMessage());
        }
        
        return "redirect:/staff/checkout";
    }
    
    // ========== CREATE BOOKING ==========
    
    @GetMapping("/create-booking")
    public String createBookingForm(Model model, Authentication authentication) {
        String username = authentication.getName();
        User staff = userRepository.findByUsername(username).orElse(null);
        
        List<User> users = userRepository.findAll();
        List<Room> rooms = roomRepository.findAll().stream()
            .filter(Room::getAvailable)
            .collect(Collectors.toList());
        
        model.addAttribute("staffName", staff != null ? staff.getFullName() : username);
        model.addAttribute("users", users);
        model.addAttribute("rooms", rooms);
        model.addAttribute("today", LocalDate.now());
        return "staff/create-booking";
    }
    
    @PostMapping("/create-booking")
    public String createBooking(@RequestParam("userId") Long userId,
                               @RequestParam("roomId") Long roomId,
                               @RequestParam("checkInDate") String checkInDate,
                               @RequestParam("checkOutDate") String checkOutDate,
                               @RequestParam("numberOfGuests") Integer numberOfGuests,
                               @RequestParam(value = "specialRequests", required = false) String specialRequests,
                               RedirectAttributes redirectAttributes) {
        
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
            
            LocalDate checkIn = LocalDate.parse(checkInDate);
            LocalDate checkOut = LocalDate.parse(checkOutDate);
            
            // Validate dates
            if (checkIn.isBefore(LocalDate.now())) {
                throw new RuntimeException("Check-in date cannot be in the past");
            }
            
            if (!checkOut.isAfter(checkIn)) {
                throw new RuntimeException("Check-out date must be after check-in date");
            }
            
            // Calculate total price
            long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
            BigDecimal totalPrice = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
            
            // Create booking
            Booking booking = new Booking();
            booking.setUser(user);
            booking.setRoom(room);
            booking.setCheckInDate(checkIn);
            booking.setCheckOutDate(checkOut);
            booking.setNumberOfGuests(numberOfGuests);
            booking.setSpecialRequests(specialRequests);
            booking.setTotalPrice(totalPrice);
            booking.setStatus("CONFIRMED");
            booking.setBookingReference("BK" + System.currentTimeMillis());
            booking.setCreatedAt(LocalDateTime.now());
            booking.setUpdatedAt(LocalDateTime.now());
            
            bookingRepository.save(booking);
            
            redirectAttributes.addFlashAttribute("success", "Booking created successfully");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create booking: " + e.getMessage());
        }
        
        return "redirect:/staff/dashboard";
    }
    
    // ========== UPDATE BOOKING STATUS ==========
    
    @PostMapping("/bookings/{id}/update-status")
    public String updateBookingStatus(@PathVariable("id") Long id, 
                                     @RequestParam("status") String status,
                                     RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            booking.setStatus(status);
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            
            // Update room availability based on status
            Room room = booking.getRoom();
            if ("CHECKED_IN".equals(status)) {
                room.setAvailable(false);
                room.setStatus(null);
            } else if ("CHECKED_OUT".equals(status) || "CANCELLED".equals(status)) {
                room.setAvailable(true);
                room.setStatus(null);
            }
            roomRepository.save(room);
            
            redirectAttributes.addFlashAttribute("success", "Booking status updated to " + status);
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update status: " + e.getMessage());
        }
        
        return "redirect:/staff/bookings/" + id;
    }
}