package com.example.hotelbooking.repository;

import com.example.hotelbooking.model.Booking;
import com.example.hotelbooking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    // ========== BASIC FIND METHODS ==========
    
    List<Booking> findByUser(User user);
    
    List<Booking> findByUserOrderByCreatedAtDesc(User user);
    
    List<Booking> findByStatus(String status);
    
    List<Booking> findByUserAndStatus(User user, String status);
    
    Optional<Booking> findByBookingReference(String bookingReference);
    
    // ========== EAGER FETCH METHODS ==========
    
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.room r " +
           "WHERE b.user = :user " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findByUserWithRoom(@Param("user") User user);
    
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.room r " +
           "WHERE b.id = :id")
    Optional<Booking> findByIdWithRoom(@Param("id") Long id);
    
    // ========== AVAILABILITY CHECKS ==========
    
    /**
     * Check if room is available for given dates
     * Returns true if no conflicting bookings exist
     */
    @Query("SELECT CASE WHEN COUNT(b) = 0 THEN true ELSE false END FROM Booking b " +
           "WHERE b.room.id = :roomId " +
           "AND b.status != 'CANCELLED' " +
           "AND ((b.checkInDate < :checkOut) AND (b.checkOutDate > :checkIn))")
    boolean isRoomAvailableForDates(@Param("roomId") Long roomId,
                                   @Param("checkIn") LocalDate checkIn,
                                   @Param("checkOut") LocalDate checkOut);
    
    /**
     * Find all overlapping bookings for a room (excluding cancelled)
     */
    @Query("SELECT b FROM Booking b " +
           "WHERE b.room.id = :roomId " +
           "AND b.status != 'CANCELLED' " +
           "AND ((b.checkInDate < :checkOut) AND (b.checkOutDate > :checkIn))")
    List<Booking> findOverlappingBookings(@Param("roomId") Long roomId,
                                         @Param("checkIn") LocalDate checkIn,
                                         @Param("checkOut") LocalDate checkOut);
    
    // ========== COUNT METHODS ==========
    
    Long countByUser(User user);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user = :user AND b.status = 'CONFIRMED'")
    Long countConfirmedByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user = :user AND b.status = 'CONFIRMED' AND b.checkInDate > :today")
    Long countUpcomingByUser(@Param("user") User user, @Param("today") LocalDate today);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user = :user AND b.checkOutDate < :today")
    Long countCompletedByUser(@Param("user") User user, @Param("today") LocalDate today);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user = :user AND b.status = 'CANCELLED'")
    Long countCancelledByUser(@Param("user") User user);
    
    // ========== DATE RANGE QUERIES ==========
    
    List<Booking> findByCheckInDateBetween(LocalDate start, LocalDate end);
    
    List<Booking> findByCheckOutDateBetween(LocalDate start, LocalDate end);
    
    @Query("SELECT b FROM Booking b WHERE b.checkInDate > :date AND b.status = 'CONFIRMED' ORDER BY b.checkInDate ASC")
    List<Booking> findUpcomingBookings(@Param("date") LocalDate date);
    
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.room WHERE b.user = :user " +
           "AND b.status = 'CONFIRMED' AND b.checkInDate > :today ORDER BY b.checkInDate ASC")
    List<Booking> findUpcomingBookingsWithRoom(@Param("user") User user, @Param("today") LocalDate today);
    
    // ========== SUMMARY QUERIES ==========
    
    @Query("SELECT SUM(b.totalPrice) FROM Booking b WHERE b.user = :user AND b.status != 'CANCELLED'")
    Long getTotalSpentByUser(@Param("user") User user);
    
    @Query("SELECT b FROM Booking b WHERE b.user = :user AND b.createdAt >= :since ORDER BY b.createdAt DESC")
    List<Booking> findRecentBookingsByUser(@Param("user") User user, @Param("since") LocalDateTime since);
    
    // ========== DELETE METHODS (for test data) ==========
    
    @Transactional
    void deleteByBookingReferenceStartingWith(String prefix);
    
    @Transactional
    void deleteByBookingReferenceContaining(String text);
}