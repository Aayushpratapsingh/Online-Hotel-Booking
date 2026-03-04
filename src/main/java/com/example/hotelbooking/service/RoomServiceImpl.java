package com.example.hotelbooking.service;

import com.example.hotelbooking.model.Room;
import com.example.hotelbooking.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoomServiceImpl implements RoomService {
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Override
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }
    
    @Override
    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }
    
    @Override
    public Room saveRoom(Room room) {
        return roomRepository.save(room);
    }
    
    @Override
    public void deleteRoom(Long id) {
        roomRepository.deleteById(id);
    }
    
    @Override
    public List<Room> getAvailableRooms() {
        return roomRepository.findByAvailableTrue();
    }
    
    @Override
    public List<Room> getRoomsByType(String type) {
        if (type == null || type.isEmpty() || type.equalsIgnoreCase("all")) {
            return getAllRooms();
        }
        // Try different repository methods
        List<Room> rooms = roomRepository.findByRoomTypeContainingIgnoreCase(type);
        if (rooms.isEmpty()) {
            rooms = roomRepository.findByRoomTypeIgnoreCase(type);
        }
        return rooms;
    }
    
    @Override
    public List<String> getDistinctRoomTypes() {
        try {
            return roomRepository.findDistinctAvailableRoomTypes();
        } catch (Exception e) {
            // Fallback if query doesn't exist
            return List.of("Single", "Double", "Deluxe", "Suite");
        }
    }
    
    @Override
    public Long getAvailableRoomsCount() {
        try {
            Long count = roomRepository.countByAvailableTrue();
            return count != null ? count : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }
}