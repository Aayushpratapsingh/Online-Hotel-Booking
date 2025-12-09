package com.example.hotelbooking.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "rooms")
public class Room {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String roomNumber;
    
    @Column(nullable = false)
    private String roomType; // SINGLE, DOUBLE, SUITE, DELUXE
    
    @Column(nullable = false)
    private String description;
    
    @Column(nullable = false)
    private BigDecimal pricePerNight;
    
    @Column(nullable = false)
    private int capacity;
    
    private boolean hasAc = false;
    private boolean hasTv = false;
    private boolean hasWifi = false;
    private boolean hasMiniBar = false;
    
    @Column(nullable = false)
    private boolean available = true;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getRoomNumber() {
        return roomNumber;
    }
    
    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }
    
    public String getRoomType() {
        return roomType;
    }
    
    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }
    
    public void setPricePerNight(BigDecimal pricePerNight) {
        this.pricePerNight = pricePerNight;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    public boolean isHasAc() {
        return hasAc;
    }
    
    public void setHasAc(boolean hasAc) {
        this.hasAc = hasAc;
    }
    
    public boolean isHasTv() {
        return hasTv;
    }
    
    public void setHasTv(boolean hasTv) {
        this.hasTv = hasTv;
    }
    
    public boolean isHasWifi() {
        return hasWifi;
    }
    
    public void setHasWifi(boolean hasWifi) {
        this.hasWifi = hasWifi;
    }
    
    public boolean isHasMiniBar() {
        return hasMiniBar;
    }
    
    public void setHasMiniBar(boolean hasMiniBar) {
        this.hasMiniBar = hasMiniBar;
    }
    
    public boolean isAvailable() {
        return available;
    }
    
    public void setAvailable(boolean available) {
        this.available = available;
    }
}