//package com.example.hotelbooking.controller;
//
//import com.example.hotelbooking.model.User;
//import com.example.hotelbooking.repository.UserRepository;
//import com.example.hotelbooking.repository.RoleRepository;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//public class TestController {
//    
//    private final UserRepository userRepository;
//    private final RoleRepository roleRepository;
//    
//    public TestController(UserRepository userRepository, RoleRepository roleRepository) {
//        this.userRepository = userRepository;
//        this.roleRepository = roleRepository;
//    }
//    
//    @GetMapping("/test-db")
//    public String testDatabase() {
//        StringBuilder result = new StringBuilder();
//        result.append("<h2>Database Test</h2>");
//        
//        // Test Users
//        result.append("<h3>Users:</h3>");
//        userRepository.findAll().forEach(user -> {
//            result.append("Username: ").append(user.getUsername())
//                  .append(" | Enabled: ").append(user.isEnabled())
//                  .append(" | Password length: ").append(user.getPassword().length())
//                  .append("<br>");
//        });
//        
//        // Test Roles
//        result.append("<h3>Roles:</h3>");
//        roleRepository.findAll().forEach(role -> {
//            result.append("Role: ").append(role.getName())
//                  .append(" | ID: ").append(role.getId())
//                  .append("<br>");
//        });
//        
//        // Test specific user
//        result.append("<h3>Admin User Details:</h3>");
//        userRepository.findByUsername("admin").ifPresentOrElse(
//            user -> {
//                result.append("Found admin user<br>");
//                result.append("Password hash: ").append(user.getPassword()).append("<br>");
//                result.append("Enabled: ").append(user.isEnabled()).append("<br>");
//                result.append("Roles: ").append(user.getRoles() != null ? user.getRoles().size() : 0).append("<br>");
//            },
//            () -> result.append("Admin user NOT found!<br>")
//        );
//        
//        return result.toString();
//    }
//    
//    @GetMapping("/test-login")
//    public String testLogin() {
//        return "<h2>Test Login</h2>" +
//               "Try to login with:<br>" +
//               "Username: admin<br>" +
//               "Password: admin123<br><br>" +
//               "<form action='/login' method='post'>" +
//               "Username: <input type='text' name='username' value='admin'><br>" +
//               "Password: <input type='password' name='password' value='admin123'><br>" +
//               "<input type='submit' value='Test Login'>" +
//               "</form>";
//    }
//}