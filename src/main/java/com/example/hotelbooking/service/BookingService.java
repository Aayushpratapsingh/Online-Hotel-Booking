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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
//    private final UserRepository userRepository;
    
    public BookingService(BookingRepository bookingRepository, 
                         RoomRepository roomRepository,
                         UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
//        this.userRepository = userRepository;
    }
    
    /**
     * Create a new booking
     */
    @Transactional
    public Booking createBooking(User user, Long roomId, 
                               LocalDate checkIn, LocalDate checkOut,
                               Integer guests, String specialRequests) {
        
        // Validate dates
        validateBookingDates(checkIn, checkOut);
        
        // Get room
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));
        
        if (!room.isAvailable()) {
            throw new RuntimeException("Room is not available");
        }
        
        // Check capacity
        if (guests > room.getCapacity()) {
            throw new RuntimeException("Room capacity is " + room.getCapacity() + " guests");
        }
        
        // Check for overlapping bookings
        if (!isRoomAvailable(roomId, checkIn, checkOut)) {
            throw new RuntimeException("Room is already booked for selected dates");
        }
        
        // Calculate total price
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal totalPrice = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
        
        // Create booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setNumberOfGuests(guests);
        booking.setTotalPrice(totalPrice);
        booking.setSpecialRequests(specialRequests);
        booking.setStatus("CONFIRMED");
        booking.setBookingReference(generateBookingReference());
        
        return bookingRepository.save(booking);
    }
    
    /**
     * Get booking by ID
     */
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
    }
    
    /**
     * Get all bookings for a user
     */
    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId);
    }
    
    /**
     * Get bookings for user with specific status
     */
    public List<Booking> getUserBookingsByStatus(Long userId, String status) {
        return bookingRepository.findByUserIdAndStatus(userId, status);
    }
    
    /**
     * Cancel a booking
     */
    @Transactional
    public Booking cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        // Check if booking can be cancelled
        if ("CANCELLED".equals(booking.getStatus())) {
            throw new RuntimeException("Booking is already cancelled");
        }
        
        if ("CHECKED_IN".equals(booking.getStatus())) {
            throw new RuntimeException("Cannot cancel booking after check-in");
        }
        
        // Check if check-in date has passed
        if (booking.getCheckInDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot cancel booking after check-in date");
        }
        
        booking.setStatus("CANCELLED");
        return bookingRepository.save(booking);
    }
    
    /**
     * Check if a room is available for given dates
     */
    public boolean isRoomAvailable(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        validateBookingDates(checkIn, checkOut);
        
        // Check room exists and is available
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));
        
        if (!room.isAvailable()) {
            return false;
        }
        
        // Check for overlapping bookings
        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
            roomId, checkIn, checkOut
        );
        
        // Filter out cancelled bookings
        overlapping = overlapping.stream()
            .filter(b -> !"CANCELLED".equals(b.getStatus()))
            .toList();
        
        return overlapping.isEmpty();
    }
    
    /**
     * Update an existing booking
     */
    @Transactional
    public Booking updateBooking(Long bookingId, LocalDate newCheckIn, LocalDate newCheckOut,
                                Integer newGuests, String specialRequests, Long userId) {
        
        validateBookingDates(newCheckIn, newCheckOut);
        
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        // Verify booking belongs to user
        if (!booking.getUser().getId().equals(userId)) {
            throw new RuntimeException("You cannot modify this booking");
        }
        
        // Check if booking can be modified
        if ("CANCELLED".equals(booking.getStatus())) {
            throw new RuntimeException("Cannot modify cancelled booking");
        }
        
        if ("CHECKED_IN".equals(booking.getStatus())) {
            throw new RuntimeException("Cannot modify booking after check-in");
        }
        
        // Check room capacity if guests changed
        if (newGuests > booking.getRoom().getCapacity()) {
            throw new RuntimeException("Room capacity is " + booking.getRoom().getCapacity() + " guests");
        }
        
        // If dates changed, check availability
        if (!booking.getCheckInDate().equals(newCheckIn) || !booking.getCheckOutDate().equals(newCheckOut)) {
            // Check if room is available for new dates (excluding this booking)
            List<Booking> overlapping = bookingRepository.findOverlappingBookingsExcluding(
                booking.getRoom().getId(), newCheckIn, newCheckOut, bookingId
            );
            
            if (!overlapping.isEmpty()) {
                throw new RuntimeException("Room is not available for new dates");
            }
            
            booking.setCheckInDate(newCheckIn);
            booking.setCheckOutDate(newCheckOut);
        }
        
        // Update other fields
        booking.setNumberOfGuests(newGuests);
        booking.setSpecialRequests(specialRequests);
        
        // Recalculate price
        long nights = ChronoUnit.DAYS.between(newCheckIn, newCheckOut);
        BigDecimal totalPrice = booking.getRoom().getPricePerNight().multiply(BigDecimal.valueOf(nights));
        booking.setTotalPrice(totalPrice);
        
        return bookingRepository.save(booking);
    }
    
    /**
     * Calculate price for a potential booking
     */
    public BigDecimal calculatePrice(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        validateBookingDates(checkIn, checkOut);
        
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));
        
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
    }
    
    /**
     * Get all active bookings (not cancelled)
     */
    public List<Booking> getActiveBookings() {
        return bookingRepository.findByStatusNot("CANCELLED");
    }
    
    /**
     * Get bookings for a specific room
     */
    public List<Booking> getBookingsByRoom(Long roomId) {
        return bookingRepository.findByRoomId(roomId);
    }
    
    /**
     * Check in a guest
     */
    @Transactional
    public Booking checkIn(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (!"CONFIRMED".equals(booking.getStatus())) {
            throw new RuntimeException("Only confirmed bookings can be checked in");
        }
        
        booking.setStatus("CHECKED_IN");
        return bookingRepository.save(booking);
    }
    
    /**
     * Check out a guest
     */
    @Transactional
    public Booking checkOut(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (!"CHECKED_IN".equals(booking.getStatus())) {
            throw new RuntimeException("Only checked-in bookings can be checked out");
        }
        
        booking.setStatus("CHECKED_OUT");
        return bookingRepository.save(booking);
    }
    
    /**
     * Get booking statistics for dashboard
     */
    public BookingStatistics getStatistics() {
        long totalBookings = bookingRepository.count();
        long activeBookings = bookingRepository.countByStatus("CONFIRMED");
        long cancelledBookings = bookingRepository.countByStatus("CANCELLED");
        BigDecimal totalRevenue = bookingRepository.sumTotalPrice();
        
        return new BookingStatistics(totalBookings, activeBookings, cancelledBookings, totalRevenue);
    }
    
    /**
     * Validate booking dates
     */
    private void validateBookingDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new RuntimeException("Check-in and check-out dates are required");
        }
        
        if (checkIn.isBefore(LocalDate.now())) {
            throw new RuntimeException("Check-in date cannot be in the past");
        }
        
        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new RuntimeException("Check-out date must be after check-in date");
        }
        
        // Maximum booking duration (30 days)
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > 30) {
            throw new RuntimeException("Maximum booking duration is 30 days");
        }
    }
    
    /**
     * Generate unique booking reference
     */
    private String generateBookingReference() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "GH" + timestamp + random;
    }
    
    /**
     * DTO for booking statistics
     */
    public static class BookingStatistics {
        private final long totalBookings;
        private final long activeBookings;
        private final long cancelledBookings;
        private final BigDecimal totalRevenue;
        
        public BookingStatistics(long totalBookings, long activeBookings, 
                                long cancelledBookings, BigDecimal totalRevenue) {
            this.totalBookings = totalBookings;
            this.activeBookings = activeBookings;
            this.cancelledBookings = cancelledBookings;
            this.totalRevenue = totalRevenue;
        }
        
        public long getTotalBookings() {
            return totalBookings;
        }
        
        public long getActiveBookings() {
            return activeBookings;
        }
        
        public long getCancelledBookings() {
            return cancelledBookings;
        }
        
        public BigDecimal getTotalRevenue() {
            return totalRevenue;
        }
    }
    
    /**
     * Search bookings by criteria
     */
    public List<Booking> searchBookings(String bookingReference, String guestName, 
                                       LocalDate checkInFrom, LocalDate checkInTo,
                                       String status) {
        if (bookingReference != null && !bookingReference.isEmpty()) {
            Optional<Booking> booking = bookingRepository.findByBookingReference(bookingReference);
            return booking.map(List::of).orElse(List.of());
        }
        
        // This would typically be a custom query
        // For simplicity, we'll filter in memory
        List<Booking> allBookings = bookingRepository.findAll();
        
        return allBookings.stream()
            .filter(booking -> guestName == null || guestName.isEmpty() || 
                     (booking.getUser().getFullName() != null && 
                      booking.getUser().getFullName().toLowerCase().contains(guestName.toLowerCase())))
            .filter(booking -> checkInFrom == null || !booking.getCheckInDate().isBefore(checkInFrom))
            .filter(booking -> checkInTo == null || !booking.getCheckInDate().isAfter(checkInTo))
            .filter(booking -> status == null || status.isEmpty() || status.equals(booking.getStatus()))
            .toList();
    }
    
    /**
     * Verify if user owns the booking
     */
    public boolean verifyUserBooking(Long bookingId, Long userId) {
        Optional<Booking> booking = bookingRepository.findById(bookingId);
        return booking.isPresent() && booking.get().getUser().getId().equals(userId);
    }
    
    /**
     * Get upcoming bookings for a user
     */
    public List<Booking> getUpcomingBookings(Long userId) {
        return bookingRepository.findByUserIdAndCheckInDateAfterAndStatus(
            userId, LocalDate.now(), "CONFIRMED");
    }
    
    /**
     * Get past bookings for a user
     */
    public List<Booking> getPastBookings(Long userId) {
        return bookingRepository.findByUserIdAndCheckOutDateBefore(userId, LocalDate.now());
    }
    
    /**
     * Get today's check-ins
     */
    public List<Booking> getTodayCheckIns() {
        return bookingRepository.findByCheckInDateAndStatus(
            LocalDate.now(), "CONFIRMED");
    }
    
    /**
     * Get today's check-outs
     */
    public List<Booking> getTodayCheckOuts() {
        return bookingRepository.findByCheckOutDateAndStatus(
            LocalDate.now(), "CHECKED_IN");
    }
    
}