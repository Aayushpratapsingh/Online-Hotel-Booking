package com.example.hotelbooking.repository;

import com.example.hotelbooking.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    
    // ========== BASIC FIND METHODS ==========
    
    /**
     * Find a room by its unique room number
     * @param roomNumber the room number to search for
     * @return Optional containing the room if found
     */
    Optional<Room> findByRoomNumber(String roomNumber);
    
    /**
     * Check if a room exists with the given room number
     * @param roomNumber the room number to check
     * @return true if a room with that number exists
     */
    boolean existsByRoomNumber(String roomNumber);
    
    /**
     * Find all available rooms
     * @return list of available rooms
     */
    List<Room> findByAvailableTrue();
    
    /**
     * Find rooms by availability status
     * @param available availability status
     * @return list of rooms with given availability
     */
    List<Room> findByAvailable(Boolean available);
    
    /**
     * Count available rooms
     * @return number of available rooms
     */
    Long countByAvailableTrue();
    
    
    // ========== ROOM TYPE SEARCH METHODS ==========
    
    /**
     * Find rooms by exact room type (case sensitive)
     * @param roomType the room type
     * @return list of rooms with that type
     */
    List<Room> findByRoomType(String roomType);
    
    /**
     * Find rooms by exact room type (case insensitive)
     * @param roomType the room type
     * @return list of rooms with that type
     */
    List<Room> findByRoomTypeIgnoreCase(String roomType);
    
    /**
     * Find rooms by room type containing text (case sensitive)
     * @param roomType partial room type
     * @return list of rooms matching
     */
    List<Room> findByRoomTypeContaining(String roomType);
    
    /**
     * Find rooms by room type containing text (case insensitive)
     * @param roomType partial room type
     * @return list of rooms matching
     */
    List<Room> findByRoomTypeContainingIgnoreCase(String roomType);
    
    /**
     * Find rooms by room type with custom query (case insensitive partial match)
     * @param type the room type to search for
     * @return list of rooms matching
     */
    @Query("SELECT r FROM Room r WHERE LOWER(r.roomType) LIKE LOWER(CONCAT('%', :type, '%'))")
    List<Room> searchByRoomType(@Param("type") String type);
    
    /**
     * Find distinct room types from available rooms
     * @return list of unique room types
     */
    @Query("SELECT DISTINCT r.roomType FROM Room r WHERE r.available = true ORDER BY r.roomType")
    List<String> findDistinctAvailableRoomTypes();
    
    
    // ========== COMBINATION FILTERS ==========
    
    /**
     * Find rooms by room type and availability
     * @param roomType the room type
     * @param available availability status
     * @return list of rooms matching
     */
    List<Room> findByRoomTypeAndAvailable(String roomType, Boolean available);
    
    /**
     * Find available rooms by room type
     * @param roomType the room type
     * @return list of available rooms with that type
     */
    List<Room> findByRoomTypeAndAvailableTrue(String roomType);
    
    
    // ========== CAPACITY FILTERS ==========
    
    /**
     * Find rooms with capacity greater than or equal to specified value
     * @param capacity minimum capacity
     * @return list of rooms meeting capacity requirement
     */
    List<Room> findByCapacityGreaterThanEqual(Integer capacity);
    
    /**
     * Find available rooms with capacity greater than or equal to specified value
     * @param capacity minimum capacity
     * @return list of available rooms meeting capacity requirement
     */
    List<Room> findByAvailableTrueAndCapacityGreaterThanEqual(Integer capacity);
    
    
    // ========== SEARCH METHODS ==========
    
    /**
     * Search available rooms by keyword in room number, type, or description
     * @param keyword the search keyword
     * @return list of matching available rooms
     */
    @Query("SELECT r FROM Room r WHERE r.available = true AND " +
           "(LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.roomType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Room> searchAvailableRooms(@Param("keyword") String keyword);
    
    /**
     * Search all rooms by keyword in room number, type, or description
     * @param keyword the search keyword
     * @return list of matching rooms
     */
    @Query("SELECT r FROM Room r WHERE " +
           "LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.roomType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Room> searchAllRooms(@Param("keyword") String keyword);
    
    
    // ========== ADVANCED FILTERS ==========
    
    /**
     * Find available rooms with multiple filters
     * @param roomType filter by room type (optional)
     * @param minPrice minimum price (optional)
     * @param maxPrice maximum price (optional)
     * @return list of matching available rooms
     */
    @Query("SELECT r FROM Room r WHERE r.available = true " +
           "AND (:roomType IS NULL OR r.roomType = :roomType) " +
           "AND (:minPrice IS NULL OR r.pricePerNight >= :minPrice) " +
           "AND (:maxPrice IS NULL OR r.pricePerNight <= :maxPrice)")
    List<Room> findAvailableRoomsWithFilters(
            @Param("roomType") String roomType,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice);
    
    /**
     * Find available rooms by room type and minimum capacity
     * @param roomType the room type
     * @param capacity minimum capacity
     * @return list of matching available rooms
     */
    List<Room> findByAvailableTrueAndRoomTypeAndCapacityGreaterThanEqual(
            String roomType, Integer capacity);
    
    
    // ========== PRICE RANGE METHODS ==========
    
    /**
     * Find rooms with price between min and max
     * @param minPrice minimum price
     * @param maxPrice maximum price
     * @return list of rooms in price range
     */
    List<Room> findByPricePerNightBetween(BigDecimal minPrice, BigDecimal maxPrice);
    
    /**
     * Find available rooms with price between min and max
     * @param minPrice minimum price
     * @param maxPrice maximum price
     * @return list of available rooms in price range
     */
    List<Room> findByAvailableTrueAndPricePerNightBetween(
            BigDecimal minPrice, BigDecimal maxPrice);
    
    
    // ========== AGGREGATION METHODS ==========
    
    /**
     * Count rooms by room type
     * @return list of objects with room type and count
     */
    @Query("SELECT r.roomType, COUNT(r) FROM Room r GROUP BY r.roomType")
    List<Object[]> countRoomsByType();
    
    /**
     * Get average price by room type
     * @return list of objects with room type and average price
     */
    @Query("SELECT r.roomType, AVG(r.pricePerNight) FROM Room r GROUP BY r.roomType")
    List<Object[]> getAveragePriceByType();
}