package com.example.hotelbooking.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "rooms")
public class Room {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "room_number", unique = true, nullable = false)
    private String roomNumber;
    
    @Column(name = "room_type", nullable = false)
    private String roomType;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private Integer capacity;
    
    @Column(name = "price_per_night", nullable = false)
    private BigDecimal pricePerNight;
    
    @Column(nullable = false)
    private Boolean available = true;
    
    @Column(name = "has_ac", nullable = false)
    private Boolean hasAc = true;
    
    @Column(name = "has_mini_bar", nullable = false)
    private Boolean hasMiniBar = true;
    
    @Column(name = "has_tv")
    private Boolean hasTv = true;
    
    @Column(name = "has_wifi")
    private Boolean hasWifi = true;
    
    @Column(name = "floor_number")
    private Integer floorNumber = 1;
    
    // ========== FIXED: ADD MISSING COLUMN ANNOTATIONS ==========
    
    @Column(name = "size_sqm")
    private Integer size;
    
    @Column(name = "bed_type", length = 50)
    private String bedType;
    
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    
    // ========== STATUS FIELD ==========
    
    @Column(name = "status", length = 20)
    private String status;
    
    // ========== CONSTRUCTORS ==========
    
    public Room() {
        // Default constructor
    }
    
    public Room(String roomNumber, String roomType, BigDecimal pricePerNight, Integer capacity) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.pricePerNight = pricePerNight;
        this.capacity = capacity;
        this.available = true;
        this.hasAc = true;
        this.hasTv = true;
        this.hasWifi = true;
        this.hasMiniBar = true;
        this.floorNumber = 1;
    }
    
    // ========== GETTERS AND SETTERS ==========
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }
    
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
    
    public Boolean getHasAc() { return hasAc; }
    public void setHasAc(Boolean hasAc) { this.hasAc = hasAc; }
    
    public Boolean getHasMiniBar() { return hasMiniBar; }
    public void setHasMiniBar(Boolean hasMiniBar) { this.hasMiniBar = hasMiniBar; }
    
    public Boolean getHasTv() { return hasTv; }
    public void setHasTv(Boolean hasTv) { this.hasTv = hasTv; }
    
    public Boolean getHasWifi() { return hasWifi; }
    public void setHasWifi(Boolean hasWifi) { this.hasWifi = hasWifi; }
    
    public Integer getFloorNumber() { return floorNumber; }
    public void setFloorNumber(Integer floorNumber) { this.floorNumber = floorNumber; }
    
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    
    public String getBedType() { return bedType; }
    public void setBedType(String bedType) { this.bedType = bedType; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    // ========== UTILITY METHODS ==========
    
    public boolean isAvailableForBooking() {
        return available != null && available && (status == null || status.isEmpty());
    }
    
    @Override
    public String toString() {
        return "Room{" +
                "id=" + id +
                ", roomNumber='" + roomNumber + '\'' +
                ", roomType='" + roomType + '\'' +
                ", pricePerNight=" + pricePerNight +
                ", capacity=" + capacity +
                ", available=" + available +
                ", status='" + status + '\'' +
                ", size=" + size +
                ", bedType='" + bedType + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}