//package com.example.hotelbooking.controller;
//
//import com.example.hotelbooking.model.User;
//import com.example.hotelbooking.model.Room;
//import com.example.hotelbooking.service.UserService;
//import com.example.hotelbooking.service.RoomService;
//import com.example.hotelbooking.service.BookingService;
//import com.example.hotelbooking.repository.UserRepository;
//import com.example.hotelbooking.repository.RoomRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//@Controller
//@RequestMapping("/admin")
//public class AdminDashboardController {
//
//    @Autowired
//    private UserRepository userRepository;
//    
//    @Autowired
//    private RoomRepository roomRepository;
//    
//    @Autowired
//    private BookingService bookingService;
//
//    @GetMapping("/dashboard")
//    public String showAdminDashboard(Model model) {
//        
//        // ========== USER STATISTICS ==========
//        List<User> allUsers = userRepository.findAll();
//        long totalUsers = allUsers.size();
//        
//        // Count new users today
//        long newUsersToday = allUsers.stream()
//                .filter(user -> {
//                    if (user.getCreatedAt() == null) return false;
//                    return user.getCreatedAt().toLocalDate().equals(java.time.LocalDate.now());
//                })
//                .count();
//        
//        // ========== ROOM STATISTICS ==========
//        List<Room> allRooms = roomRepository.findAll();
//        long totalRooms = allRooms.size();
//        
//        // Count available rooms
//        long availableRooms = allRooms.stream()
//                .filter(room -> room.getAvailable() != null && room.getAvailable())
//                .count();
//        
//        // ========== BOOKING STATISTICS ==========
//        List<com.example.hotelbooking.model.Booking> allBookings = 
//            bookingService.getAllBookings(); // We need to add this method
//        
//        long totalBookings = allBookings.size();
//        
//        long confirmedBookings = allBookings.stream()
//                .filter(b -> "CONFIRMED".equals(b.getStatus()))
//                .count();
//        
//        long pendingBookings = allBookings.stream()
//                .filter(b -> "PENDING".equals(b.getStatus()))
//                .count();
//        
//        long cancelledBookings = allBookings.stream()
//                .filter(b -> "CANCELLED".equals(b.getStatus()))
//                .count();
//        
//        // ========== REVENUE STATISTICS ==========
//        BigDecimal totalRevenue = allBookings.stream()
//                .filter(b -> !"CANCELLED".equals(b.getStatus()))
//                .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//        
//        BigDecimal todayRevenue = allBookings.stream()
//                .filter(b -> !"CANCELLED".equals(b.getStatus()))
//                .filter(b -> {
//                    if (b.getCreatedAt() == null) return false;
//                    return b.getCreatedAt().toLocalDate().equals(java.time.LocalDate.now());
//                })
//                .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//        
//        // Add all data to model
//        model.addAttribute("totalUsers", totalUsers);
//        model.addAttribute("newUsersToday", newUsersToday);
//        model.addAttribute("totalRooms", totalRooms);
//        model.addAttribute("availableRooms", availableRooms);
//        model.addAttribute("totalBookings", totalBookings);
//        model.addAttribute("confirmedBookings", confirmedBookings);
//        model.addAttribute("pendingBookings", pendingBookings);
//        model.addAttribute("cancelledBookings", cancelledBookings);
//        model.addAttribute("totalRevenue", totalRevenue);
//        model.addAttribute("todayRevenue", todayRevenue);
//        
//        // Add current date for last updated
//        model.addAttribute("currentDate", new java.util.Date());
//        
//        return "admin/dashboard";
//    }
//}