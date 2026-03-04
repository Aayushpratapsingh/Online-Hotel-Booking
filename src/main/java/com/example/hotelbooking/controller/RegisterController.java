//package com.example.hotelbooking.controller;
//
//import com.example.hotelbooking.model.Role;
//import com.example.hotelbooking.model.User;
//import com.example.hotelbooking.repository.RoleRepository;
//import com.example.hotelbooking.repository.UserRepository;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//import java.util.HashSet;
//import java.util.Set;
//
//@Controller
//public class RegisterController {
//    
//    private final UserRepository userRepository;
//    private final RoleRepository roleRepository;
//    private final PasswordEncoder passwordEncoder;
//    
//    public RegisterController(UserRepository userRepository, 
//                             RoleRepository roleRepository,
//                             PasswordEncoder passwordEncoder) {
//        this.userRepository = userRepository;
//        this.roleRepository = roleRepository;
//        this.passwordEncoder = passwordEncoder;
//    }
//    
//    @GetMapping("/register")
//    public String showRegistrationForm(Model model) {
//        return "register";
//    }
//    
//    @PostMapping("/register")
//    public String registerUser(
//            @RequestParam String username,
//            @RequestParam String fullName,
//            @RequestParam String email,
//            @RequestParam String password,
//            @RequestParam String confirmPassword,
//            RedirectAttributes redirectAttributes) {
//        
//        // Check if passwords match
//        if (!password.equals(confirmPassword)) {
//            redirectAttributes.addFlashAttribute("error", "Passwords do not match!");
//            return "redirect:/register";
//        }
//        
//        // Check if username exists
//        if (userRepository.findByUsername(username).isPresent()) {
//            redirectAttributes.addFlashAttribute("error", "Username already exists!");
//            return "redirect:/register";
//        }
//        
//        // Check if email exists
//        if (userRepository.findByEmail(email).isPresent()) {
//            redirectAttributes.addFlashAttribute("error", "Email already registered!");
//            return "redirect:/register";
//        }
//        
//        try {
//            // Create new user
//            User user = new User();
//            user.setUsername(username);
//            user.setFullName(fullName);
//            user.setEmail(email);
//            user.setPassword(passwordEncoder.encode(password));
//            user.setEnabled(true);
//            
//            // Assign ROLE_USER by default
//            Role userRole = roleRepository.findByName("ROLE_USER")
//                .orElseGet(() -> {
//                    Role newRole = new Role("ROLE_USER");
//                    return roleRepository.save(newRole);
//                });
//            
//            Set<Role> roles = new HashSet<>();
//            roles.add(userRole);
//            user.setRoles(roles);
//            
//            // Save user
//            userRepository.save(user);
//            
//            redirectAttributes.addFlashAttribute("success", 
//                "Registration successful! Please login.");
//            return "redirect:/login";
//            
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", 
//                "Registration failed: " + e.getMessage());
//            return "redirect:/register";
//        }
//    }
//}