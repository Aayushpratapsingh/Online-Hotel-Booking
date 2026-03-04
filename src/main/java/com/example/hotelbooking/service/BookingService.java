package com.example.hotelbooking.service;

import com.example.hotelbooking.model.Booking;
import com.example.hotelbooking.model.Room;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.RoomRepository;
import com.example.hotelbooking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    
    public BookingService(BookingRepository bookingRepository, 
                         RoomRepository roomRepository,
                         UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }
    
    @Transactional
    public Booking createBooking(User user, Long roomId, 
                               LocalDate checkInDate, LocalDate checkOutDate, 
                               Integer numberOfGuests, String specialRequests) {
        
        System.out.println("========== CREATING BOOKING ==========");
        System.out.println("User: " + user.getUsername() + " (ID: " + user.getId() + ")");
        System.out.println("Room ID: " + roomId);
        System.out.println("Check-in: " + checkInDate);
        System.out.println("Check-out: " + checkOutDate);
        System.out.println("Guests: " + numberOfGuests);
        
        // Validate dates
        if (checkInDate == null || checkOutDate == null) {
            throw new RuntimeException("Check-in and check-out dates are required");
        }
        
        if (checkInDate.isBefore(LocalDate.now())) {
            throw new RuntimeException("Check-in date cannot be in the past");
        }
        
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new RuntimeException("Check-out date must be after check-in date");
        }
        
        // Get room
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found with ID: " + roomId));
        
        System.out.println("Room found: " + room.getRoomNumber() + " - " + room.getRoomType());
        System.out.println("Price per night: $" + room.getPricePerNight());
        
        // Check room availability
        if (room.getAvailable() == null || !room.getAvailable()) {
            throw new RuntimeException("Room is not available for booking");
        }
        
        // Check capacity
        if (numberOfGuests > room.getCapacity()) {
            throw new RuntimeException("Maximum guests for this room is " + room.getCapacity());
        }
        
        // Check date availability
        boolean available = bookingRepository.isRoomAvailableForDates(roomId, checkInDate, checkOutDate);
        System.out.println("Room available for dates: " + available);
        
        if (!available) {
            throw new RuntimeException("Room is not available for the selected dates");
        }
        
        // Calculate price
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (nights <= 0) nights = 1;
        
        BigDecimal totalPrice = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
        System.out.println("Nights: " + nights);
        System.out.println("Total price: $" + totalPrice);
        
        // Create new booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setCheckInDate(checkInDate);
        booking.setCheckOutDate(checkOutDate);
        booking.setNumberOfGuests(numberOfGuests);
        booking.setSpecialRequests(specialRequests);
        booking.setTotalPrice(totalPrice);
        booking.setStatus("CONFIRMED");
        
        // Generate booking reference
        String bookingRef = "BK" + System.currentTimeMillis() + 
                           UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        booking.setBookingReference(bookingRef);
        
        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        
        System.out.println("Saving booking with reference: " + bookingRef);
        
        // Save booking
        Booking savedBooking = bookingRepository.save(booking);
        
        // Flush to ensure it's written to database
        bookingRepository.flush();
        
        System.out.println("Booking saved successfully!");
        System.out.println("Booking ID: " + savedBooking.getId());
        System.out.println("Booking Reference: " + savedBooking.getBookingReference());
        System.out.println("========================================");
        
        return savedBooking;
    }
    
    /**
     * NEW METHOD: Get all bookings for admin dashboard
     */
    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        System.out.println("========== GET ALL BOOKINGS ==========");
        
        List<Booking> bookings = bookingRepository.findAll();
        System.out.println("Total bookings in database: " + bookings.size());
        
        // Log all bookings for debugging
        for (Booking b : bookings) {
            System.out.println("  - ID: " + b.getId() + 
                              ", Ref: " + b.getBookingReference() + 
                              ", Status: " + b.getStatus() + 
                              ", Price: $" + b.getTotalPrice() +
                              ", Created: " + b.getCreatedAt());
        }
        
        // Filter out test data if needed
        bookings = bookings.stream()
            .filter(b -> !b.getBookingReference().startsWith("DEBUG"))
            .filter(b -> !b.getBookingReference().startsWith("SIMPLE"))
            .collect(Collectors.toList());
        
        System.out.println("After filtering test data: " + bookings.size());
        System.out.println("========================================");
        
        return bookings;
    }
    
    @Transactional(readOnly = true)
    public boolean isRoomAvailable(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            return false;
        }
        if (checkIn.isBefore(LocalDate.now())) {
            return false;
        }
        if (!checkOut.isAfter(checkIn)) {
            return false;
        }
        return bookingRepository.isRoomAvailableForDates(roomId, checkIn, checkOut);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal calculatePrice(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new RuntimeException("Check-in and check-out dates are required");
        }
        
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));
        
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) nights = 1;
        
        return room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
    }
    
    @Transactional
    public Booking cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if ("CANCELLED".equals(booking.getStatus())) {
            throw new RuntimeException("Booking is already cancelled");
        }
        
        if (booking.getCheckInDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot cancel past bookings");
        }
        
        booking.setStatus("CANCELLED");
        booking.setUpdatedAt(LocalDateTime.now());
        
        return bookingRepository.save(booking);
    }
    
    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
    }
    
    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(User user) {
        System.out.println("========== GET USER BOOKINGS ==========");
        System.out.println("User ID: " + user.getId() + ", Username: " + user.getUsername());
        
        List<Booking> bookings = bookingRepository.findByUserWithRoom(user);
        System.out.println("Raw bookings from DB: " + bookings.size());
        
        // Log all bookings before filtering
        for (Booking b : bookings) {
            System.out.println("  - Found: ID=" + b.getId() + 
                              ", Ref=" + b.getBookingReference() + 
                              ", Status=" + b.getStatus() + 
                              ", Created=" + b.getCreatedAt());
        }
        
        // Filter out DEBUG bookings (test data)
        bookings = bookings.stream()
            .filter(b -> !b.getBookingReference().startsWith("DEBUG"))
            .filter(b -> !b.getBookingReference().startsWith("SIMPLE"))
            .collect(Collectors.toList());
        
        System.out.println("After filtering: " + bookings.size());
        
        // Initialize transient fields for display
        bookings.forEach(Booking::initializeTransientFields);
        
        return bookings;
    }
    
    @Transactional(readOnly = true)
    public List<Booking> getRecentBookings(User user, int limit) {
        System.out.println("========== GET RECENT BOOKINGS ==========");
        System.out.println("User: " + user.getUsername());
        System.out.println("Limit: " + limit);
        
        List<Booking> bookings = bookingRepository.findByUserWithRoom(user);
        System.out.println("Total bookings found: " + bookings.size());
        
        // Log all bookings
        for (Booking b : bookings) {
            System.out.println("  - Found: " + b.getBookingReference() + 
                              " (Created: " + b.getCreatedAt() + ")");
        }
        
        // Filter out DEBUG and SIMPLE test bookings
        bookings = bookings.stream()
            .filter(b -> !b.getBookingReference().startsWith("DEBUG"))
            .filter(b -> !b.getBookingReference().startsWith("SIMPLE"))
            .collect(Collectors.toList());
        
        System.out.println("After filtering: " + bookings.size());
        
        // Initialize transient fields
        bookings.forEach(Booking::initializeTransientFields);
        
        // Sort by created date descending and limit
        List<Booking> recent = bookings.stream()
            .sorted((b1, b2) -> {
                if (b1.getCreatedAt() == null) return 1;
                if (b2.getCreatedAt() == null) return -1;
                return b2.getCreatedAt().compareTo(b1.getCreatedAt());
            })
            .limit(limit)
            .collect(Collectors.toList());
        
        System.out.println("Returning " + recent.size() + " recent bookings:");
        for (Booking b : recent) {
            System.out.println("  - Returning: " + b.getBookingReference() + 
                              " (Created: " + b.getCreatedAt() + ")");
        }
        System.out.println("========================================");
        
        return recent;
    }
    
    @Transactional(readOnly = true)
    public DashboardStatisticsWithSpent getUserDashboardStatsWithSpent(User user) {
        List<Booking> bookings = getUserBookings(user);
        
        long totalBookings = bookings.size();
        long upcomingBookings = bookings.stream()
            .filter(b -> "CONFIRMED".equals(b.getStatus()) && b.isUpcoming())
            .count();
        long completedBookings = bookings.stream()
            .filter(Booking::isCompleted)
            .count();
        long confirmedBookings = bookings.stream()
            .filter(b -> "CONFIRMED".equals(b.getStatus()))
            .count();
        
        // Calculate total spent (excluding cancelled)
        BigDecimal totalSpent = bookings.stream()
            .filter(b -> !"CANCELLED".equals(b.getStatus()))
            .map(Booking::getTotalPrice)
            .filter(price -> price != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        System.out.println("========== DASHBOARD STATS ==========");
        System.out.println("User: " + user.getUsername());
        System.out.println("Total Bookings: " + totalBookings);
        System.out.println("Upcoming: " + upcomingBookings);
        System.out.println("Completed: " + completedBookings);
        System.out.println("Confirmed: " + confirmedBookings);
        System.out.println("Total Spent: $" + totalSpent);
        System.out.println("=====================================");
        
        return new DashboardStatisticsWithSpent(
            totalBookings,
            upcomingBookings,
            completedBookings,
            confirmedBookings,
            totalSpent
        );
    }
    
    // DTO Classes
    public static class DashboardStatistics {
        private final long totalBookings;
        private final long upcomingBookings;
        private final long completedBookings;
        private final long confirmedBookings;
        
        public DashboardStatistics(long totalBookings, long upcomingBookings, 
                                  long completedBookings, long confirmedBookings) {
            this.totalBookings = totalBookings;
            this.upcomingBookings = upcomingBookings;
            this.completedBookings = completedBookings;
            this.confirmedBookings = confirmedBookings;
        }
        
        public long getTotalBookings() { return totalBookings; }
        public long getUpcomingBookings() { return upcomingBookings; }
        public long getCompletedBookings() { return completedBookings; }
        public long getConfirmedBookings() { return confirmedBookings; }
    }
    
    public static class DashboardStatisticsWithSpent {
        private final long totalBookings;
        private final long upcomingBookings;
        private final long completedBookings;
        private final long confirmedBookings;
        private final BigDecimal totalSpent;
        
        public DashboardStatisticsWithSpent(long totalBookings, long upcomingBookings, 
                                          long completedBookings, long confirmedBookings,
                                          BigDecimal totalSpent) {
            this.totalBookings = totalBookings;
            this.upcomingBookings = upcomingBookings;
            this.completedBookings = completedBookings;
            this.confirmedBookings = confirmedBookings;
            this.totalSpent = totalSpent;
        }
        
        public long getTotalBookings() { return totalBookings; }
        public long getUpcomingBookings() { return upcomingBookings; }
        public long getCompletedBookings() { return completedBookings; }
        public long getConfirmedBookings() { return confirmedBookings; }
        public BigDecimal getTotalSpent() { return totalSpent; }
    }
}