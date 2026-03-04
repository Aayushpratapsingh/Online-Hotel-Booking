package com.example.hotelbooking.service;

import com.example.hotelbooking.model.Booking;
import com.example.hotelbooking.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class StatisticsService {
    
    private final BookingRepository bookingRepository;
    
    public StatisticsService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }
    
    public BookingStatistics getStatistics() {
        List<Booking> allBookings = bookingRepository.findAll();
        
        long totalBookings = allBookings.size();
        long activeBookings = 0;
        long cancelledBookings = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        
        for (Booking booking : allBookings) {
            String status = booking.getStatus();
            
            if ("CANCELLED".equals(status)) {
                cancelledBookings++;
            } else if ("CONFIRMED".equals(status) || "CHECKED_IN".equals(status)) {
                activeBookings++;
            }
            
            // Sum revenue for completed bookings
            if ("CONFIRMED".equals(status) || "CHECKED_IN".equals(status) || "CHECKED_OUT".equals(status)) {
                if (booking.getTotalPrice() != null) {
                    totalRevenue = totalRevenue.add(booking.getTotalPrice());
                }
            }
        }
        
        return new BookingStatistics(totalBookings, activeBookings, cancelledBookings, totalRevenue);
    }
    
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
        
        // Getters...
    }
}