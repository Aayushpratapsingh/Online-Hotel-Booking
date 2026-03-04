package com.example.hotelbooking.controller;

import com.example.hotelbooking.model.Room;
import com.example.hotelbooking.service.RoomService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private RoomService roomService;

    @GetMapping({"/", "/home"})
    public String home(Model model, HttpServletRequest request,
                      @RequestParam(value = "logout", required = false) String logout,
                      @RequestParam(value = "error", required = false) String error) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() 
                && !"anonymousUser".equals(auth.getName());
        
        System.out.println("=== HOME PAGE DEBUG INFO ===");
        System.out.println("User: " + auth.getName());
        System.out.println("Is Authenticated: " + isAuthenticated);
        
        model.addAttribute("isAuthenticated", isAuthenticated);
        
        if (isAuthenticated) {
            String username = auth.getName();
            model.addAttribute("username", username);
            
            // Get user roles
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            boolean isAdmin = false;
            boolean isStaff = false;
            boolean isUser = false;
            
            System.out.println("User Authorities:");
            for (GrantedAuthority authority : authorities) {
                String role = authority.getAuthority();
                System.out.println("  - " + role);
                
                // Check with and without ROLE_ prefix
                if (role.equals("ROLE_ADMIN") || role.equals("ADMIN")) {
                    isAdmin = true;
                } else if (role.equals("ROLE_STAFF") || role.equals("STAFF")) {
                    isStaff = true;
                } else if (role.equals("ROLE_USER") || role.equals("USER")) {
                    isUser = true;
                }
            }
            
            System.out.println("isAdmin: " + isAdmin);
            System.out.println("isStaff: " + isStaff);
            System.out.println("isUser: " + isUser);
            System.out.println("canBook: " + isUser);
            
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("isStaff", isStaff);
            model.addAttribute("isUser", isUser);
            
            // USER can book (even if they have other roles)
            model.addAttribute("canBook", isUser);
        } else {
            model.addAttribute("canBook", false);
        }
        
        // Get available rooms for home page
        try {
            List<Room> rooms = roomService.getAvailableRooms();
            model.addAttribute("rooms", rooms);
            
            // Get distinct room types for filter
            List<String> roomTypes = roomService.getDistinctRoomTypes();
            model.addAttribute("roomTypes", roomTypes);
            
            System.out.println("Loaded " + rooms.size() + " available rooms for home page");
            System.out.println("Room types available: " + roomTypes);
        } catch (Exception e) {
            System.out.println("Error loading rooms: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("rooms", List.of()); // Empty list on error
            model.addAttribute("roomTypes", List.of());
        }
        
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully.");
            model.addAttribute("messageType", "success");
        }
        
        if (error != null) {
            model.addAttribute("message", "Login failed. Please try again.");
            model.addAttribute("messageType", "error");
        }
        
        return "user/home";
    }
    
    @GetMapping("/rooms")
    public String roomsPage(Model model,
                           @RequestParam(value = "type", required = false) String roomType,
                           @RequestParam(value = "available", required = false) Boolean availableOnly) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() 
                && !"anonymousUser".equals(auth.getName());
        
        System.out.println("=== ROOMS PAGE DEBUG INFO ===");
        System.out.println("User: " + auth.getName());
        System.out.println("Is Authenticated: " + isAuthenticated);
        
        model.addAttribute("isAuthenticated", isAuthenticated);
        
        if (isAuthenticated) {
            String username = auth.getName();
            model.addAttribute("username", username);
            
            // Get user roles
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            boolean isAdmin = false;
            boolean isStaff = false;
            boolean isUser = false;
            
            System.out.println("User Authorities:");
            for (GrantedAuthority authority : authorities) {
                String role = authority.getAuthority();
                System.out.println("  - " + role);
                
                // Check with and without ROLE_ prefix
                if (role.equals("ROLE_ADMIN") || role.equals("ADMIN")) {
                    isAdmin = true;
                } else if (role.equals("ROLE_STAFF") || role.equals("STAFF")) {
                    isStaff = true;
                } else if (role.equals("ROLE_USER") || role.equals("USER")) {
                    isUser = true;
                }
            }
            
            System.out.println("isAdmin: " + isAdmin);
            System.out.println("isStaff: " + isStaff);
            System.out.println("isUser: " + isUser);
            System.out.println("canBook: " + isUser);
            
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("isStaff", isStaff);
            model.addAttribute("isUser", isUser);
            
            // USER can book (even if they have other roles)
            model.addAttribute("canBook", isUser);
        } else {
            model.addAttribute("canBook", false);
        }
        
        // Get room types for filter dropdown
        List<String> roomTypes = roomService.getDistinctRoomTypes();
        model.addAttribute("roomTypes", roomTypes);
        
        // Get rooms based on filters
        List<Room> rooms;
        try {
            if (roomType != null && !roomType.isEmpty() && !roomType.equalsIgnoreCase("all")) {
                rooms = roomService.getRoomsByType(roomType);
                model.addAttribute("selectedType", roomType);
                System.out.println("Filtering by room type: " + roomType);
            } else if (availableOnly != null && availableOnly) {
                rooms = roomService.getAvailableRooms();
                model.addAttribute("availableOnly", true);
                System.out.println("Filtering: Available rooms only");
            } else {
                rooms = roomService.getAllRooms();
                System.out.println("Loading all rooms (no filter)");
            }
            
            model.addAttribute("rooms", rooms);
            System.out.println("Loaded " + rooms.size() + " rooms for rooms page");
            
        } catch (Exception e) {
            System.out.println("Error loading rooms: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("rooms", List.of()); // Empty list on error
        }
        
        return "user/rooms";
    }
}