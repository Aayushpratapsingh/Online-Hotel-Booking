package com.example.hotelbooking.config;

import com.example.hotelbooking.model.Role;
import com.example.hotelbooking.model.Room;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.repository.RoleRepository;
import com.example.hotelbooking.repository.RoomRepository;
import com.example.hotelbooking.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;
    
    public DataInitializer(RoleRepository roleRepository, 
                          UserRepository userRepository,
                          RoomRepository roomRepository,
                          PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public void run(String... args) throws Exception {
        // Create roles if they don't exist
        if (roleRepository.count() == 0) {
            Role adminRole = new Role("ROLE_ADMIN");
            Role userRole = new Role("ROLE_USER");
            Role staffRole = new Role("ROLE_STAFF");
            
            roleRepository.save(adminRole);
            roleRepository.save(userRole);
            roleRepository.save(staffRole);
            
            System.out.println("✓ Roles created successfully!");
        }
        
        // Create admin user if doesn't exist
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@hotel.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Administrator");
            admin.setEnabled(true);
            
            // Assign roles
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("Role not found"));
            Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Role not found"));
            
            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            roles.add(userRole);
            admin.setRoles(roles);
            
            userRepository.save(admin);
            System.out.println("✓ Admin user created successfully!");
            
            // Create a regular user for testing
            User testUser = new User();
            testUser.setUsername("user");
            testUser.setEmail("user@hotel.com");
            testUser.setPassword(passwordEncoder.encode("user123"));
            testUser.setFullName("Test User");
            testUser.setEnabled(true);
            
            Set<Role> userRoles = new HashSet<>();
            userRoles.add(userRole);
            testUser.setRoles(userRoles);
            
            userRepository.save(testUser);
            System.out.println("✓ Test user created (username: user, password: user123)");
            
            // Create a staff user for testing
            User staffUser = new User();
            staffUser.setUsername("staff");
            staffUser.setEmail("staff@hotel.com");
            staffUser.setPassword(passwordEncoder.encode("staff123"));
            staffUser.setFullName("Staff Member");
            staffUser.setEnabled(true);
            
            Role staffRole = roleRepository.findByName("ROLE_STAFF")
                .orElseThrow(() -> new RuntimeException("Role not found"));
            Set<Role> staffRoles = new HashSet<>();
            staffRoles.add(staffRole);
            staffUser.setRoles(staffRoles);
            
            userRepository.save(staffUser);
            System.out.println("✓ Staff user created (username: staff, password: staff123)");
        }
        
        // Create sample rooms if none exist
        if (roomRepository.count() == 0) {
            System.out.println("Creating sample rooms...");
            
            // Room 1: Deluxe Room
            Room room1 = new Room();
            room1.setRoomNumber("101");
            room1.setRoomType("Deluxe Room");
            room1.setDescription("Luxury deluxe room with king bed and city view");
            room1.setCapacity(2);
            room1.setPricePerNight(new BigDecimal("299.99"));
            room1.setAvailable(true);
            room1.setHasAc(true);
            room1.setHasMiniBar(true);
            room1.setHasTv(true);
            room1.setHasWifi(true);
            room1.setFloorNumber(1);
            roomRepository.save(room1);
            
            // Room 2: Deluxe Room
            Room room2 = new Room();
            room2.setRoomNumber("102");
            room2.setRoomType("Deluxe Room");
            room2.setDescription("Luxury deluxe room with king bed");
            room2.setCapacity(2);
            room2.setPricePerNight(new BigDecimal("299.99"));
            room2.setAvailable(true);
            room2.setHasAc(true);
            room2.setHasMiniBar(true);
            room2.setHasTv(true);
            room2.setHasWifi(true);
            room2.setFloorNumber(1);
            roomRepository.save(room2);
            
            // Room 3: Executive Suite
            Room room3 = new Room();
            room3.setRoomNumber("201");
            room3.setRoomType("Executive Suite");
            room3.setDescription("Executive suite with separate living area");
            room3.setCapacity(3);
            room3.setPricePerNight(new BigDecimal("459.99"));
            room3.setAvailable(true);
            room3.setHasAc(true);
            room3.setHasMiniBar(true);
            room3.setHasTv(true);
            room3.setHasWifi(true);
            room3.setFloorNumber(2);
            roomRepository.save(room3);
            
            // Room 4: Executive Suite (Unavailable)
            Room room4 = new Room();
            room4.setRoomNumber("202");
            room4.setRoomType("Executive Suite");
            room4.setDescription("Executive suite with separate living area");
            room4.setCapacity(3);
            room4.setPricePerNight(new BigDecimal("459.99"));
            room4.setAvailable(false);
            room4.setHasAc(true);
            room4.setHasMiniBar(true);
            room4.setHasTv(true);
            room4.setHasWifi(true);
            room4.setFloorNumber(2);
            roomRepository.save(room4);
            
            // Room 5: Presidential Suite
            Room room5 = new Room();
            room5.setRoomNumber("301");
            room5.setRoomType("Presidential Suite");
            room5.setDescription("Presidential suite with private balcony and butler service");
            room5.setCapacity(4);
            room5.setPricePerNight(new BigDecimal("899.99"));
            room5.setAvailable(true);
            room5.setHasAc(true);
            room5.setHasMiniBar(true);
            room5.setHasTv(true);
            room5.setHasWifi(true);
            room5.setFloorNumber(3);
            roomRepository.save(room5);
            
            // Room 6: Single Room
            Room room6 = new Room();
            room6.setRoomNumber("302");
            room6.setRoomType("Single Room");
            room6.setDescription("Standard single room with queen bed");
            room6.setCapacity(1);
            room6.setPricePerNight(new BigDecimal("149.99"));
            room6.setAvailable(true);
            room6.setHasAc(true);
            room6.setHasMiniBar(false); // Single rooms might not have mini bar
            room6.setHasTv(true);
            room6.setHasWifi(true);
            room6.setFloorNumber(3);
            roomRepository.save(room6);
            
            // Room 7: Double Room
            Room room7 = new Room();
            room7.setRoomNumber("303");
            room7.setRoomType("Double Room");
            room7.setDescription("Standard double room with two double beds");
            room7.setCapacity(2);
            room7.setPricePerNight(new BigDecimal("199.99"));
            room7.setAvailable(true);
            room7.setHasAc(true);
            room7.setHasMiniBar(false); // Double rooms might not have mini bar
            room7.setHasTv(true);
            room7.setHasWifi(true);
            room7.setFloorNumber(3);
            roomRepository.save(room7);
            
            // Room 8: Economy Room (no AC)
            Room room8 = new Room();
            room8.setRoomNumber("104");
            room8.setRoomType("Economy Room");
            room8.setDescription("Basic economy room for budget travelers");
            room8.setCapacity(1);
            room8.setPricePerNight(new BigDecimal("99.99"));
            room8.setAvailable(true);
            room8.setHasAc(false); // Economy room might not have AC
            room8.setHasMiniBar(false);
            room8.setHasTv(true);
            room8.setHasWifi(true);
            room8.setFloorNumber(1);
            roomRepository.save(room8);
            
            // Room 9: Family Suite
            Room room9 = new Room();
            room9.setRoomNumber("203");
            room9.setRoomType("Family Suite");
            room9.setDescription("Family suite with connecting rooms");
            room9.setCapacity(5);
            room9.setPricePerNight(new BigDecimal("599.99"));
            room9.setAvailable(true);
            room9.setHasAc(true);
            room9.setHasMiniBar(true);
            room9.setHasTv(true);
            room9.setHasWifi(true);
            room9.setFloorNumber(2);
            roomRepository.save(room9);
            
            System.out.println("✓ Created " + roomRepository.count() + " sample rooms");
            System.out.println("✓ Available room types: Deluxe Room, Executive Suite, Presidential Suite, Single Room, Double Room, Economy Room, Family Suite");
            System.out.println("✓ Test credentials:");
            System.out.println("  - Admin: admin / admin123");
            System.out.println("  - User: user / user123");
            System.out.println("  - Staff: staff / staff123");
        } else {
            System.out.println("✓ Database already contains " + roomRepository.count() + " rooms");
        }
    }
}