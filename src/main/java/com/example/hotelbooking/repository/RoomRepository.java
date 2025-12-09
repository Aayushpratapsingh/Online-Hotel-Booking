package com.example.hotelbooking.repository;

import com.example.hotelbooking.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    
    List<Room> findByAvailableTrue();
    
    List<Room> findByRoomType(String roomType);
    
    List<Room> findByPricePerNightBetween(BigDecimal minPrice, BigDecimal maxPrice);
    
    List<Room> findByCapacityGreaterThanEqual(int capacity);
    
    @Query("SELECT r FROM Room r WHERE r.available = true AND r.capacity >= :guests")
    List<Room> findAvailableRoomsForGuests(@Param("guests") int guests);
    
    @Query("SELECT DISTINCT r.roomType FROM Room r")
    List<String> findAllRoomTypes();
}