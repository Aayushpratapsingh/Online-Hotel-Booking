package com.example.hotelbooking.controller;

import com.example.hotelbooking.model.User;
import com.example.hotelbooking.model.Role;
import com.example.hotelbooking.model.Room;
import com.example.hotelbooking.model.Booking;
import com.example.hotelbooking.repository.UserRepository;
import com.example.hotelbooking.repository.RoleRepository;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.RoomRepository;
import com.example.hotelbooking.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookingService bookingService;

    public AdminController(UserRepository userRepository,
                           RoleRepository roleRepository,
                           BookingRepository bookingRepository,
                           RoomRepository roomRepository,
                           PasswordEncoder passwordEncoder,
                           BookingService bookingService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.passwordEncoder = passwordEncoder;
        this.bookingService = bookingService;
    }

 // ================= DASHBOARD =================
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        try {
            // Get REAL data from database
            long totalUsers = userRepository.count();
            long totalBookings = bookingRepository.count();
            long totalRooms = roomRepository.count();
            
            // Calculate REAL revenue (excluding cancelled bookings)
            BigDecimal revenue = BigDecimal.ZERO;
            List<Booking> allBookings = bookingRepository.findAll();
            for (Booking booking : allBookings) {
                // Only include non-cancelled bookings in revenue
                if (booking.getTotalPrice() != null && !"CANCELLED".equals(booking.getStatus())) {
                    revenue = revenue.add(booking.getTotalPrice());
                }
            }
            
            // Add REAL data to model
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("totalBookings", totalBookings);
            model.addAttribute("totalRooms", totalRooms);
            model.addAttribute("revenue", revenue);
            model.addAttribute("template", "admin/dashboard"); // This tells layout to include dashboard.html
            
            System.out.println("=== DASHBOARD DATA ===");
            System.out.println("Total Users: " + totalUsers);
            System.out.println("Total Bookings: " + totalBookings);
            System.out.println("Total Rooms: " + totalRooms);
            System.out.println("Total Revenue: $" + revenue);
            
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error loading dashboard: " + e.getMessage());
        }
        
        return "admin/layout"; // Returns the layout with dashboard content
    }

    // ================= USERS MANAGEMENT =================
    
    @GetMapping("/users")
    public String listUsers(Model model) {
        try {
            List<User> users = userRepository.findAll();
            model.addAttribute("users", users);
            model.addAttribute("template", "admin/users");
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error loading users: " + e.getMessage());
        }
        return "admin/layout";
    }

    // ... rest of your controller methods remain exactly the same ...
    
    @GetMapping("/users/create")
    public String showCreateUserForm(Model model) {
        User user = new User();
        user.setEnabled(true);
        model.addAttribute("user", user);
        model.addAttribute("roleNames", List.of("USER", "STAFF", "ADMIN"));
        model.addAttribute("template", "admin/create-user");
        return "admin/layout";
    }

    @PostMapping("/users/create")
    public String createUser(
            @ModelAttribute("user") User user,
            @RequestParam(value = "role", defaultValue = "USER") String roleName,
            @RequestParam(value = "password") String password,
            RedirectAttributes redirectAttributes) {

        try {
            // Validation
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Username is required!");
                return "redirect:/admin/users/create";
            }

            if (password == null || password.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Password is required!");
                return "redirect:/admin/users/create";
            }

            if (password.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters!");
                return "redirect:/admin/users/create";
            }

            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Email is required!");
                return "redirect:/admin/users/create";
            }

            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Username already exists!");
                return "redirect:/admin/users/create";
            }

            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Email already exists!");
                return "redirect:/admin/users/create";
            }

            // Create user
            user.setPassword(passwordEncoder.encode(password));
            user.setEnabled(true);
            
            Set<Role> roles = new HashSet<>();
            Role role = roleRepository.findByName("ROLE_" + roleName)
                    .orElseGet(() -> {
                        Role newRole = new Role("ROLE_" + roleName);
                        return roleRepository.save(newRole);
                    });
            roles.add(role);
            user.setRoles(roles);
            
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("success", "User created successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error creating user: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }

    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

            String currentRole = user.getRoles().stream()
                    .findFirst()
                    .map(role -> role.getName().replace("ROLE_", ""))
                    .orElse("USER");
            
            model.addAttribute("user", user);
            model.addAttribute("currentRole", currentRole);
            model.addAttribute("roleNames", List.of("USER", "STAFF", "ADMIN"));
            model.addAttribute("template", "admin/edit-user");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error loading user: " + e.getMessage());
            return "redirect:/admin/users";
        }
        
        return "admin/layout";
    }

    @PostMapping("/users/update/{id}")
    public String updateUser(
            @PathVariable("id") Long id,
            @ModelAttribute("user") User updatedUser,
            @RequestParam(value = "role", defaultValue = "USER") String roleName,
            @RequestParam(value = "enabled", defaultValue = "true") boolean enabled,
            RedirectAttributes redirectAttributes) {

        try {
            User existingUser = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

            // Check email uniqueness
            if (!existingUser.getEmail().equals(updatedUser.getEmail()) &&
                    userRepository.findByEmail(updatedUser.getEmail()).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Email already exists!");
                return "redirect:/admin/users/edit/" + id;
            }

            // Update user
            existingUser.setUsername(updatedUser.getUsername());
            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setEnabled(enabled);
            
            // Update role
            Set<Role> roles = new HashSet<>();
            Role role = roleRepository.findByName("ROLE_" + roleName)
                    .orElseGet(() -> {
                        Role newRole = new Role("ROLE_" + roleName);
                        return roleRepository.save(newRole);
                    });
            roles.add(role);
            existingUser.setRoles(roles);

            userRepository.save(existingUser);
            redirectAttributes.addFlashAttribute("success", "User updated successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error updating user: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

            // Prevent deleting default admin
            boolean isAdmin = user.getRoles().stream()
                    .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));
            
            if (isAdmin && "admin".equalsIgnoreCase(user.getUsername())) {
                redirectAttributes.addFlashAttribute("error", "Cannot delete default admin user!");
                return "redirect:/admin/users";
            }

            userRepository.delete(user);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error deleting user: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }

    @GetMapping("/users/toggle/{id}")
    public String toggleUserStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

            // Prevent toggling default admin
            boolean isAdmin = user.getRoles().stream()
                    .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));
            
            if (isAdmin && "admin".equalsIgnoreCase(user.getUsername())) {
                redirectAttributes.addFlashAttribute("error", "Cannot disable default admin user!");
                return "redirect:/admin/users";
            }

            user.setEnabled(!user.isEnabled());
            userRepository.save(user);

            String status = user.isEnabled() ? "enabled" : "disabled";
            redirectAttributes.addFlashAttribute("success", "User " + user.getUsername() + " has been " + status + "!");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error toggling user status: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }

    // ================= BOOKINGS MANAGEMENT =================
    
    @GetMapping("/bookings")
    public String adminBookings(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
            Model model) {
        
        try {
            System.out.println("=== LOADING BOOKINGS PAGE ===");
            System.out.println("Search: " + search);
            System.out.println("Status: " + status);
            System.out.println("From Date: " + fromDate);
            System.out.println("To Date: " + toDate);
            
            List<Booking> allBookings = bookingRepository.findAll();
            
            // Apply filters
            List<Booking> filteredBookings = allBookings;
            
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase();
                filteredBookings = filteredBookings.stream()
                    .filter(b -> {
                        if (b.getUser() != null) {
                            String username = b.getUser().getUsername() != null ? b.getUser().getUsername().toLowerCase() : "";
                            String email = b.getUser().getEmail() != null ? b.getUser().getEmail().toLowerCase() : "";
                            String bookingRef = b.getBookingReference() != null ? b.getBookingReference().toLowerCase() : "";
                            return username.contains(searchLower) || 
                                   email.contains(searchLower) ||
                                   bookingRef.contains(searchLower) ||
                                   (b.getId() != null && b.getId().toString().contains(search));
                        }
                        return b.getId() != null && b.getId().toString().contains(search);
                    })
                    .collect(Collectors.toList());
            }
            
            if (status != null && !status.trim().isEmpty() && !status.equals("all")) {
                String finalStatus = status;
                filteredBookings = filteredBookings.stream()
                    .filter(b -> finalStatus.equals(b.getStatus()))
                    .collect(Collectors.toList());
            }
            
            if (fromDate != null) {
                LocalDate finalFromDate = fromDate;
                filteredBookings = filteredBookings.stream()
                    .filter(b -> b.getCheckInDate() != null && !b.getCheckInDate().isBefore(finalFromDate))
                    .collect(Collectors.toList());
            }
            
            if (toDate != null) {
                LocalDate finalToDate = toDate;
                filteredBookings = filteredBookings.stream()
                    .filter(b -> b.getCheckOutDate() != null && !b.getCheckOutDate().isAfter(finalToDate))
                    .collect(Collectors.toList());
            }
            
            // Calculate stats
            long totalBookings = allBookings.size();
            long activeBookings = allBookings.stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()) || "CHECKED_IN".equals(b.getStatus()))
                .count();
            long pendingBookings = allBookings.stream()
                .filter(b -> "PENDING".equals(b.getStatus()))
                .count();
            
            BigDecimal todayRevenue = allBookings.stream()
                .filter(b -> b.getCheckInDate() != null && 
                            b.getCheckInDate().equals(LocalDate.now()) && 
                            b.getTotalPrice() != null)
                .map(Booking::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            model.addAttribute("bookings", filteredBookings);
            model.addAttribute("totalBookings", totalBookings);
            model.addAttribute("activeBookings", activeBookings);
            model.addAttribute("pendingBookings", pendingBookings);
            model.addAttribute("todayRevenue", todayRevenue);
            model.addAttribute("search", search);
            model.addAttribute("status", status);
            model.addAttribute("fromDate", fromDate);
            model.addAttribute("toDate", toDate);
            model.addAttribute("template", "admin/bookings");
            
            System.out.println("=== BOOKINGS PAGE LOADED SUCCESSFULLY ===");
            System.out.println("Filtered bookings count: " + filteredBookings.size());
            
        } catch (Exception e) {
            System.out.println("=== ERROR LOADING BOOKINGS PAGE ===");
            e.printStackTrace();
            model.addAttribute("error", "Error loading bookings: " + e.getMessage());
            model.addAttribute("bookings", List.of());
            model.addAttribute("totalBookings", 0);
            model.addAttribute("activeBookings", 0);
            model.addAttribute("pendingBookings", 0);
            model.addAttribute("todayRevenue", BigDecimal.ZERO);
            model.addAttribute("template", "admin/bookings");
        }
        
        return "admin/layout";
    }
    
    @PostMapping("/bookings/{id}/status")
    public String updateBookingStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            RedirectAttributes redirectAttributes) {
        
        try {
            System.out.println("=== UPDATING BOOKING STATUS ===");
            System.out.println("Booking ID: " + id);
            System.out.println("New Status: " + status);
            
            Booking booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + id));
            
            booking.setStatus(status);
            bookingRepository.save(booking);
            
            redirectAttributes.addFlashAttribute("success", "Booking status updated to " + status + "!");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error updating booking status: " + e.getMessage());
        }
        
        return "redirect:/admin/bookings";
    }
    
    @PostMapping("/bookings/{id}/cancel")
    public String cancelBooking(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {
        
        try {
            System.out.println("=== CANCELLING BOOKING ===");
            System.out.println("Booking ID: " + id);
            
            Booking booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + id));
            
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);
            
            redirectAttributes.addFlashAttribute("success", "Booking cancelled successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error cancelling booking: " + e.getMessage());
        }
        
        return "redirect:/admin/bookings";
    }
    
    @PostMapping("/bookings/{id}/delete")
    public String deleteBooking(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {
        
        try {
            System.out.println("=== DELETING BOOKING ===");
            System.out.println("Booking ID: " + id);
            
            Booking booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + id));
            
            bookingRepository.delete(booking);
            redirectAttributes.addFlashAttribute("success", "Booking deleted successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error deleting booking: " + e.getMessage());
        }
        
        return "redirect:/admin/bookings";
    }
    
    // ================= ROOMS MANAGEMENT =================
    
    @GetMapping("/rooms")
    public String adminRooms(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            Model model) {
        
        try {
            System.out.println("=== LOADING ROOMS PAGE ===");
            System.out.println("Search: " + search);
            System.out.println("Type: " + type);
            System.out.println("Status: " + status);
            
            // Get all rooms
            List<Room> rooms = roomRepository.findAll();
            System.out.println("Total rooms found: " + rooms.size());
            
            // Calculate statistics
            long totalRooms = rooms.size();
            long availableRooms = rooms.stream()
                    .filter(room -> room.getAvailable() != null && room.getAvailable())
                    .count();
            long totalCapacity = rooms.stream()
                    .mapToInt(room -> room.getCapacity() != null ? room.getCapacity() : 0)
                    .sum();
            
            // Get unique room types
            List<String> roomTypes = rooms.stream()
                    .map(Room::getRoomType)
                    .filter(roomType -> roomType != null && !roomType.isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            
            // Apply filters
            List<Room> filteredRooms = rooms;
            
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase();
                filteredRooms = filteredRooms.stream()
                    .filter(room -> 
                        (room.getRoomNumber() != null && room.getRoomNumber().toLowerCase().contains(searchLower)) ||
                        (room.getDescription() != null && room.getDescription().toLowerCase().contains(searchLower)) ||
                        (room.getRoomType() != null && room.getRoomType().toLowerCase().contains(searchLower)))
                    .collect(Collectors.toList());
            }
            
            if (type != null && !type.trim().isEmpty() && !type.equals("all") && !type.equals("")) {
                String finalType = type;
                filteredRooms = filteredRooms.stream()
                    .filter(room -> room.getRoomType() != null && room.getRoomType().equals(finalType))
                    .collect(Collectors.toList());
            }
            
            if (status != null && !status.trim().isEmpty() && !status.equals("all") && !status.equals("")) {
                if (status.equals("available")) {
                    filteredRooms = filteredRooms.stream()
                        .filter(room -> room.getAvailable() != null && room.getAvailable())
                        .collect(Collectors.toList());
                } else if (status.equals("booked") || status.equals("unavailable")) {
                    filteredRooms = filteredRooms.stream()
                        .filter(room -> room.getAvailable() == null || !room.getAvailable())
                        .collect(Collectors.toList());
                }
            }
            
            model.addAttribute("rooms", filteredRooms);
            model.addAttribute("totalRooms", totalRooms);
            model.addAttribute("availableRooms", availableRooms);
            model.addAttribute("bookedRooms", totalRooms - availableRooms);
            model.addAttribute("totalCapacity", totalCapacity);
            model.addAttribute("roomTypes", roomTypes);
            model.addAttribute("template", "admin/rooms");
            
            System.out.println("=== ROOMS PAGE LOADED SUCCESSFULLY ===");
            System.out.println("Filtered rooms count: " + filteredRooms.size());
            
        } catch (Exception e) {
            System.out.println("=== ERROR LOADING ROOMS PAGE ===");
            e.printStackTrace();
            model.addAttribute("error", "Error loading rooms: " + e.getMessage());
            model.addAttribute("rooms", List.of());
            model.addAttribute("totalRooms", 0);
            model.addAttribute("availableRooms", 0);
            model.addAttribute("bookedRooms", 0);
            model.addAttribute("totalCapacity", 0);
            model.addAttribute("roomTypes", List.of());
            model.addAttribute("template", "admin/rooms");
        }
        
        return "admin/layout";
    }
    
    @GetMapping("/rooms/create")
    public String showCreateRoomForm(Model model) {
        Room room = new Room();
        // Set defaults
        room.setAvailable(true);
        room.setHasAc(true);
        room.setHasTv(true);
        room.setHasWifi(true);
        room.setHasMiniBar(true);
        
        model.addAttribute("room", room);
        model.addAttribute("roomTypes", List.of("Standard", "Deluxe", "Suite", "Executive", "Family", "Presidential"));
        model.addAttribute("bedTypes", List.of("Single", "Double", "Queen", "King", "Twin"));
        model.addAttribute("template", "admin/create-room");
        
        return "admin/layout";
    }
    
    @PostMapping("/rooms/create")
    public String createRoom(
            @ModelAttribute Room room,
            RedirectAttributes redirectAttributes) {
        
        try {
            System.out.println("=== CREATING NEW ROOM ===");
            System.out.println("Room Number: " + room.getRoomNumber());
            System.out.println("Room Type: " + room.getRoomType());
            System.out.println("Price: " + room.getPricePerNight());
            System.out.println("Capacity: " + room.getCapacity());
            System.out.println("Size: " + room.getSize());
            System.out.println("Bed Type: " + room.getBedType());
            System.out.println("Image URL: " + room.getImageUrl());
            
            // Validate required fields
            if (room.getRoomNumber() == null || room.getRoomNumber().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Room number is required!");
                return "redirect:/admin/rooms/create";
            }
            
            if (roomRepository.existsByRoomNumber(room.getRoomNumber())) {
                redirectAttributes.addFlashAttribute("error", "Room number already exists!");
                return "redirect:/admin/rooms/create";
            }
            
            if (room.getRoomType() == null || room.getRoomType().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Room type is required!");
                return "redirect:/admin/rooms/create";
            }
            
            if (room.getPricePerNight() == null || room.getPricePerNight().compareTo(BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("error", "Price must be greater than 0!");
                return "redirect:/admin/rooms/create";
            }
            
            if (room.getCapacity() == null || room.getCapacity() <= 0) {
                redirectAttributes.addFlashAttribute("error", "Capacity must be greater than 0!");
                return "redirect:/admin/rooms/create";
            }
            
            if (room.getSize() == null || room.getSize() <= 0) {
                redirectAttributes.addFlashAttribute("error", "Room size must be greater than 0!");
                return "redirect:/admin/rooms/create";
            }
            
            if (room.getBedType() == null || room.getBedType().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Bed type is required!");
                return "redirect:/admin/rooms/create";
            }
            
            // Handle boolean fields (checkboxes send null when unchecked)
            room.setHasAc(room.getHasAc() != null ? room.getHasAc() : false);
            room.setHasTv(room.getHasTv() != null ? room.getHasTv() : false);
            room.setHasWifi(room.getHasWifi() != null ? room.getHasWifi() : false);
            room.setHasMiniBar(room.getHasMiniBar() != null ? room.getHasMiniBar() : false);
            room.setAvailable(room.getAvailable() != null ? room.getAvailable() : true);
            
            // Set default floor if not provided
            if (room.getFloorNumber() == null) {
                room.setFloorNumber(1);
            }
            
            // Save the room
            Room savedRoom = roomRepository.save(room);
            
            System.out.println("=== ROOM CREATED SUCCESSFULLY WITH ID: " + savedRoom.getId() + " ===");
            redirectAttributes.addFlashAttribute("success", "Room created successfully!");
            
        } catch (Exception e) {
            System.out.println("=== ERROR CREATING ROOM ===");
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error creating room: " + e.getMessage());
        }
        
        return "redirect:/admin/rooms";
    }
    
    @GetMapping("/rooms/edit/{id}")
    public String showEditRoomForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== LOADING EDIT FORM FOR ROOM ID: " + id + " ===");
            
            Room room = roomRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Room not found with ID: " + id));
            
            System.out.println("Room found: " + room.getRoomNumber());
            System.out.println("Room size: " + room.getSize());
            System.out.println("Bed type: " + room.getBedType());
            System.out.println("Image URL: " + room.getImageUrl());
            
            model.addAttribute("room", room);
            model.addAttribute("roomTypes", List.of("Standard", "Deluxe", "Suite", "Executive", "Family", "Presidential"));
            model.addAttribute("bedTypes", List.of("Single", "Double", "Queen", "King", "Twin"));
            model.addAttribute("template", "admin/edit-room");
            
            System.out.println("=== EDIT FORM LOADED SUCCESSFULLY ===");
            return "admin/layout";
            
        } catch (Exception e) {
            System.out.println("=== ERROR LOADING EDIT FORM ===");
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error loading room: " + e.getMessage());
            return "redirect:/admin/rooms";
        }
    }
    
    @PostMapping("/rooms/update/{id}")
    public String updateRoom(
            @PathVariable("id") Long id,
            @ModelAttribute Room updatedRoom,
            RedirectAttributes redirectAttributes) {
        
        try {
            System.out.println("=== UPDATING ROOM ID: " + id + " ===");
            System.out.println("Room Number: " + updatedRoom.getRoomNumber());
            System.out.println("Room Type: " + updatedRoom.getRoomType());
            System.out.println("Price: " + updatedRoom.getPricePerNight());
            System.out.println("Capacity: " + updatedRoom.getCapacity());
            System.out.println("Size: " + updatedRoom.getSize());
            System.out.println("Bed Type: " + updatedRoom.getBedType());
            System.out.println("Image URL: " + updatedRoom.getImageUrl());
            System.out.println("Has AC: " + updatedRoom.getHasAc());
            System.out.println("Has TV: " + updatedRoom.getHasTv());
            System.out.println("Has WiFi: " + updatedRoom.getHasWifi());
            System.out.println("Has Mini Bar: " + updatedRoom.getHasMiniBar());
            System.out.println("Available: " + updatedRoom.getAvailable());
            
            // Find existing room
            Room existingRoom = roomRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Room not found with ID: " + id));
            
            // Check if room number is being changed and if it already exists
            if (!existingRoom.getRoomNumber().equals(updatedRoom.getRoomNumber()) &&
                    roomRepository.existsByRoomNumber(updatedRoom.getRoomNumber())) {
                redirectAttributes.addFlashAttribute("error", "Room number already exists!");
                return "redirect:/admin/rooms/edit/" + id;
            }
            
            // Update all fields
            existingRoom.setRoomNumber(updatedRoom.getRoomNumber());
            existingRoom.setRoomType(updatedRoom.getRoomType());
            existingRoom.setDescription(updatedRoom.getDescription());
            existingRoom.setPricePerNight(updatedRoom.getPricePerNight());
            existingRoom.setCapacity(updatedRoom.getCapacity());
            existingRoom.setSize(updatedRoom.getSize());
            existingRoom.setBedType(updatedRoom.getBedType());
            existingRoom.setImageUrl(updatedRoom.getImageUrl());
            
            // Handle boolean fields (checkboxes send null when unchecked)
            existingRoom.setHasAc(updatedRoom.getHasAc() != null ? updatedRoom.getHasAc() : false);
            existingRoom.setHasTv(updatedRoom.getHasTv() != null ? updatedRoom.getHasTv() : false);
            existingRoom.setHasWifi(updatedRoom.getHasWifi() != null ? updatedRoom.getHasWifi() : false);
            existingRoom.setHasMiniBar(updatedRoom.getHasMiniBar() != null ? updatedRoom.getHasMiniBar() : false);
            existingRoom.setAvailable(updatedRoom.getAvailable() != null ? updatedRoom.getAvailable() : false);
            
            // Update floor if provided
            if (updatedRoom.getFloorNumber() != null) {
                existingRoom.setFloorNumber(updatedRoom.getFloorNumber());
            }
            
            // Update status if provided
            if (updatedRoom.getStatus() != null) {
                existingRoom.setStatus(updatedRoom.getStatus());
            }
            
            // Save the updated room
            roomRepository.save(existingRoom);
            
            System.out.println("=== ROOM UPDATED SUCCESSFULLY ===");
            redirectAttributes.addFlashAttribute("success", "Room updated successfully!");
            
        } catch (Exception e) {
            System.out.println("=== ERROR UPDATING ROOM ===");
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error updating room: " + e.getMessage());
        }
        
        return "redirect:/admin/rooms";
    }
    
    @GetMapping("/rooms/toggle/{id}")
    public String toggleRoomAvailability(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {
        
        try {
            System.out.println("=== TOGGLING ROOM AVAILABILITY ID: " + id + " ===");
            
            Room room = roomRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Room not found with ID: " + id));
            
            // Toggle availability
            room.setAvailable(!room.getAvailable());
            roomRepository.save(room);
            
            String status = room.getAvailable() ? "available" : "unavailable";
            System.out.println("Room " + room.getRoomNumber() + " is now " + status);
            
            redirectAttributes.addFlashAttribute("success", 
                "Room " + room.getRoomNumber() + " is now " + status + "!");
            
        } catch (Exception e) {
            System.out.println("=== ERROR TOGGLING ROOM AVAILABILITY ===");
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error toggling room availability: " + e.getMessage());
        }
        
        return "redirect:/admin/rooms";
    }
    
    @GetMapping("/rooms/delete/{id}")
    public String deleteRoom(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {
        
        try {
            System.out.println("=== DELETING ROOM ID: " + id + " ===");
            
            Room room = roomRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Room not found with ID: " + id));
            
            // Check if room has any bookings
            boolean hasBookings = bookingRepository.findAll().stream()
                    .anyMatch(booking -> booking.getRoom() != null && 
                                        booking.getRoom().getId().equals(id) &&
                                        !"CANCELLED".equals(booking.getStatus()));
            
            if (hasBookings) {
                redirectAttributes.addFlashAttribute("error", 
                    "Cannot delete room with active bookings! Please cancel or complete bookings first.");
                return "redirect:/admin/rooms";
            }
            
            String roomNumber = room.getRoomNumber();
            roomRepository.delete(room);
            
            System.out.println("Room " + roomNumber + " deleted successfully");
            redirectAttributes.addFlashAttribute("success", "Room " + roomNumber + " deleted successfully!");
            
        } catch (Exception e) {
            System.out.println("=== ERROR DELETING ROOM ===");
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error deleting room: " + e.getMessage());
        }
        
        return "redirect:/admin/rooms";
    }
    
 // ================= REPORTS =================
    @GetMapping("/reports")
    public String adminReports(
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
            Model model) {
        
        try {
            System.out.println("=== LOADING REPORTS PAGE ===");
            
            // Set default dates if not provided
            LocalDate effectiveFromDate = fromDate;
            LocalDate effectiveToDate = toDate;
            
            if (effectiveFromDate == null) {
                effectiveFromDate = LocalDate.now().minusDays(30);
            }
            if (effectiveToDate == null) {
                effectiveToDate = LocalDate.now();
            }
            
            // Create FINAL copies for lambda expressions
            final LocalDate finalFromDate = effectiveFromDate;
            final LocalDate finalToDate = effectiveToDate;
            
            // Get all bookings
            List<Booking> allBookings = bookingRepository.findAll();
            
            // Filter bookings by date range
            List<Booking> filteredBookings = allBookings.stream()
                .filter(b -> b.getCreatedAt() != null)
                .filter(b -> !b.getCreatedAt().toLocalDate().isBefore(finalFromDate))
                .filter(b -> !b.getCreatedAt().toLocalDate().isAfter(finalToDate))
                .collect(Collectors.toList());
            
            // Calculate statistics
            long totalBookings = filteredBookings.size();
            
            long confirmedBookings = filteredBookings.stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .count();
                
            long checkedInBookings = filteredBookings.stream()
                .filter(b -> "CHECKED_IN".equals(b.getStatus()))
                .count();
                
            long checkedOutBookings = filteredBookings.stream()
                .filter(b -> "CHECKED_OUT".equals(b.getStatus()))
                .count();
                
            long cancelledBookings = filteredBookings.stream()
                .filter(b -> "CANCELLED".equals(b.getStatus()))
                .count();
                
            long pendingBookings = filteredBookings.stream()
                .filter(b -> "PENDING".equals(b.getStatus()))
                .count();
            
            // Calculate revenue (exclude cancelled)
            BigDecimal totalRevenue = filteredBookings.stream()
                .filter(b -> !"CANCELLED".equals(b.getStatus()))
                .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Room statistics
            long totalRooms = roomRepository.count();
            long availableRooms = roomRepository.findAll().stream()
                .filter(room -> room.getAvailable() != null && room.getAvailable())
                .count();
            
            // Calculate occupancy rate
            double occupancyRate = totalRooms > 0 ? 
                ((totalRooms - availableRooms) * 100.0 / totalRooms) : 0.0;
            
            // ========== INSERT THE MODEL ATTRIBUTES HERE ==========
            // Add ALL attributes to model
            model.addAttribute("totalRevenue", totalRevenue);
            model.addAttribute("totalBookings", totalBookings);
            model.addAttribute("confirmedBookings", confirmedBookings);
            model.addAttribute("checkedInBookings", checkedInBookings);
            model.addAttribute("checkedOutBookings", checkedOutBookings);
            model.addAttribute("cancelledBookings", cancelledBookings);
            model.addAttribute("pendingBookings", pendingBookings);
            model.addAttribute("totalRooms", totalRooms);
            model.addAttribute("availableRooms", availableRooms);
            model.addAttribute("occupancyRate", occupancyRate);
            model.addAttribute("fromDate", effectiveFromDate);
            model.addAttribute("toDate", effectiveToDate);
            model.addAttribute("currentDate", new java.util.Date());
            model.addAttribute("template", "admin/reports");
            // ========== END OF MODEL ATTRIBUTES ==========
            
            System.out.println("=== REPORTS DATA ===");
            System.out.println("Total Revenue: $" + totalRevenue);
            System.out.println("Total Bookings: " + totalBookings);
            System.out.println("Confirmed: " + confirmedBookings);
            System.out.println("Checked-in: " + checkedInBookings);
            System.out.println("Checked-out: " + checkedOutBookings);
            System.out.println("Cancelled: " + cancelledBookings);
            System.out.println("Pending: " + pendingBookings);
            System.out.println("Occupancy Rate: " + occupancyRate + "%");
            
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error loading reports: " + e.getMessage());
        }
        
        return "admin/layout";
    }
    // ================= SETTINGS =================
    
    @GetMapping("/settings")
    public String adminSettings(Model model) {
        try {
            // Add any settings data here
            model.addAttribute("template", "admin/settings");
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error loading settings: " + e.getMessage());
        }
        return "admin/layout";
    }
}