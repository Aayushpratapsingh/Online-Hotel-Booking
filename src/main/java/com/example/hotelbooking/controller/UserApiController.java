/*
 * package com.example.hotelbooking.controller;
 * 
 * import com.example.hotelbooking.model.User; import
 * com.example.hotelbooking.service.BookingService; import
 * org.springframework.security.access.prepost.PreAuthorize; import
 * org.springframework.web.bind.annotation.GetMapping; import
 * org.springframework.web.bind.annotation.RequestMapping; import
 * org.springframework.web.bind.annotation.RestController;
 * 
 * import java.security.Principal; import java.time.LocalDateTime; import
 * java.util.HashMap; import java.util.Map;
 * 
 * @RestController
 * 
 * @RequestMapping("/api/user")
 * 
 * @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'STAFF')") public class
 * UserApiController {
 * 
 * private final BookingService bookingService; private final
 * com.example.hotelbooking.repository.UserRepository userRepository;
 * 
 * public UserApiController(BookingService bookingService,
 * com.example.hotelbooking.repository.UserRepository userRepository) {
 * this.bookingService = bookingService; this.userRepository = userRepository; }
 * 
 * @GetMapping("/dashboard/stats") public Map<String, Object>
 * getDashboardStats(Principal principal) { String username =
 * principal.getName(); User user = userRepository.findByUsername(username)
 * .orElseThrow(() -> new RuntimeException("User not found"));
 * 
 * Long userId = user.getId(); Map<String, Object> response = new HashMap<>();
 * 
 * try { BookingService.UserBookingStats userStats =
 * bookingService.getUserBookingStats(userId);
 * 
 * response.put("success", true); response.put("totalBookings",
 * userStats.getTotalBookings()); response.put("activeBookings",
 * userStats.getActiveBookings()); response.put("totalSpent",
 * userStats.getTotalSpent()); response.put("upcomingCheckins",
 * bookingService.getUpcomingCheckinsForUser(userId, 7).size());
 * response.put("lastUpdated", LocalDateTime.now().toString());
 * 
 * } catch (Exception e) { response.put("success", false); response.put("error",
 * "Unable to fetch updates"); }
 * 
 * return response; } }
 */