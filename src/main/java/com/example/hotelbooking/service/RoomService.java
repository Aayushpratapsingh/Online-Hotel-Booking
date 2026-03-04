package com.example.hotelbooking.service;

import com.example.hotelbooking.model.Room;
import java.util.List;
import java.util.Optional;

public interface RoomService {
    List<Room> getAllRooms();
    Optional<Room> getRoomById(Long id);
    Room saveRoom(Room room);
    void deleteRoom(Long id);
    List<Room> getAvailableRooms();
    List<Room> getRoomsByType(String type);  // ✅ This method will use the new repository method
    List<String> getDistinctRoomTypes();
    Long getAvailableRoomsCount();
}