package com.example.hotelbooking.repository;

import com.example.hotelbooking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    // Find bookings by user ID
    List<Booking> findByUserId(Long userId);
    
    // Find bookings by user ID and status
    List<Booking> findByUserIdAndStatus(Long userId, String status);
    
    // Find booking by reference number
    Optional<Booking> findByBookingReference(String bookingReference);
    
    // Find overlapping bookings for a room
    @Query("SELECT b FROM Booking b WHERE " +
           "b.room.id = :roomId AND " +
           "b.status NOT IN ('CANCELLED', 'CHECKED_OUT') AND " +
           "b.checkInDate <= :checkOut AND b.checkOutDate >= :checkIn")
    List<Booking> findOverlappingBookings(
        @Param("roomId") Long roomId,
        @Param("checkIn") LocalDate checkIn,
        @Param("checkOut") LocalDate checkOut
    );
    
    // Find overlapping bookings excluding a specific booking
    @Query("SELECT b FROM Booking b WHERE " +
           "b.room.id = :roomId AND " +
           "b.id != :excludeBookingId AND " +
           "b.status NOT IN ('CANCELLED', 'CHECKED_OUT') AND " +
           "b.checkInDate <= :checkOut AND b.checkOutDate >= :checkIn")
    List<Booking> findOverlappingBookingsExcluding(
        @Param("roomId") Long roomId,
        @Param("checkIn") LocalDate checkIn,
        @Param("checkOut") LocalDate checkOut,
        @Param("excludeBookingId") Long excludeBookingId
    );
    
    // Count methods
    Long countByUserId(Long userId);
    Long countByUserIdAndStatus(Long userId, String status);
    Long countByStatus(String status);
    
    // Find by status
    List<Booking> findByStatus(String status);
    
    // Find by status not equal
    List<Booking> findByStatusNot(String status);
    
    // Find by room ID
    List<Booking> findByRoomId(Long roomId);
    
    // Calculate total revenue
    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b WHERE b.status NOT IN ('CANCELLED')")
    BigDecimal sumTotalPrice();
    
    // Find upcoming bookings for user
    List<Booking> findByUserIdAndCheckInDateAfterAndStatus(Long userId, LocalDate date, String status);
    
    // Find past bookings for user
    List<Booking> findByUserIdAndCheckOutDateBefore(Long userId, LocalDate date);
    
    // Find today's check-ins
    List<Booking> findByCheckInDateAndStatus(LocalDate checkInDate, String status);
    
    // Find today's check-outs
    List<Booking> findByCheckOutDateAndStatus(LocalDate checkOutDate, String status);
    
    // Optional: Search bookings by date range
    @Query("SELECT b FROM Booking b WHERE " +
           "b.checkInDate BETWEEN :startDate AND :endDate " +
           "OR b.checkOutDate BETWEEN :startDate AND :endDate")
    List<Booking> findBookingsBetweenDates(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}