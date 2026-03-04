package com.example.hotelbooking.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "bookings")
public class Booking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
    
    @Column(name = "booking_reference", unique = true, length = 50)
    private String bookingReference;
    
    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;
    
    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;
    
    @Column(name = "number_of_guests")
    private Integer numberOfGuests;
    
    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;
    
    @Column(name = "status", length = 20)
    private String status;
    
    @Column(name = "special_requests", length = 1000)
    private String specialRequests;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Transient fields for display
    @Transient
    private String formattedCheckInDate;
    
    @Transient
    private String formattedCheckOutDate;
    
    @Transient
    private String roomTypeDisplay;
    
    @Transient
    private Long nights;
    
    // Constructors
    public Booking() {
        this.status = "CONFIRMED";
    }
    
    public Booking(User user, Room room, LocalDate checkInDate, LocalDate checkOutDate, 
                  Integer numberOfGuests, BigDecimal totalPrice) {
        this.user = user;
        this.room = room;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.numberOfGuests = numberOfGuests;
        this.totalPrice = totalPrice;
        this.status = "CONFIRMED";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.bookingReference = generateBookingReference();
    }
    
    private String generateBookingReference() {
        return "BK" + System.currentTimeMillis() + (user != null ? user.getId().toString().substring(0, 2) : "00");
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (bookingReference == null) {
            bookingReference = "BK" + System.currentTimeMillis() + 
                              (user != null ? user.getId().toString().substring(0, Math.min(2, user.getId().toString().length())) : "00");
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Helper methods
    public void initializeTransientFields() {
        if (checkInDate != null) {
            this.formattedCheckInDate = checkInDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        }
        if (checkOutDate != null) {
            this.formattedCheckOutDate = checkOutDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        }
        if (room != null) {
            this.roomTypeDisplay = room.getRoomType();
        }
        if (checkInDate != null && checkOutDate != null) {
            this.nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
            if (this.nights <= 0) this.nights = 1L;
        }
    }
    
    public boolean isUpcoming() {
        return checkInDate != null && checkInDate.isAfter(LocalDate.now());
    }
    
    public boolean isCompleted() {
        return checkOutDate != null && checkOutDate.isBefore(LocalDate.now());
    }
    
    public boolean isActive() {
        LocalDate now = LocalDate.now();
        return checkInDate != null && checkOutDate != null &&
               !checkInDate.isAfter(now) && !checkOutDate.isBefore(now);
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    
    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }
    
    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }
    
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }
    
    public Integer getNumberOfGuests() { return numberOfGuests; }
    public void setNumberOfGuests(Integer numberOfGuests) { this.numberOfGuests = numberOfGuests; }
    
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Transient getters/setters
    public String getFormattedCheckInDate() { 
        if (formattedCheckInDate == null && checkInDate != null) {
            formattedCheckInDate = checkInDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        }
        return formattedCheckInDate; 
    }
    public void setFormattedCheckInDate(String formattedCheckInDate) { this.formattedCheckInDate = formattedCheckInDate; }
    
    public String getFormattedCheckOutDate() { 
        if (formattedCheckOutDate == null && checkOutDate != null) {
            formattedCheckOutDate = checkOutDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        }
        return formattedCheckOutDate; 
    }
    public void setFormattedCheckOutDate(String formattedCheckOutDate) { this.formattedCheckOutDate = formattedCheckOutDate; }
    
    public String getRoomTypeDisplay() { 
        if (roomTypeDisplay == null && room != null) {
            roomTypeDisplay = room.getRoomType();
        }
        return roomTypeDisplay; 
    }
    public void setRoomTypeDisplay(String roomTypeDisplay) { this.roomTypeDisplay = roomTypeDisplay; }
    
    public Long getNights() { 
        if (nights == null && checkInDate != null && checkOutDate != null) {
            nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
            if (nights <= 0) nights = 1L;
        }
        return nights; 
    }
    public void setNights(Long nights) { this.nights = nights; }
}