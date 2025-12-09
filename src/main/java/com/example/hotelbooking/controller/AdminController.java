package com.example.hotelbooking.controller;

import com.example.hotelbooking.model.User;
import com.example.hotelbooking.model.Role;
import com.example.hotelbooking.repository.UserRepository;
import com.example.hotelbooking.repository.RoleRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    
    public AdminController(UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalBookings", 0);
        model.addAttribute("totalRooms", 0);
        model.addAttribute("revenue", "$0");
        return "admin/dashboard";
    }
    
    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin/users";
    }
    
    @GetMapping("/users/create")
    public String showCreateUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("allRoles", roleRepository.findAll());
        return "admin/create-user";
    }
    
    @PostMapping("/users/create")
    public String createUser(
            @ModelAttribute("user") User user,
            @RequestParam("password") String password,
            @RequestParam(value = "roleIds", required = false) List<Long> roleIds,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        System.out.println("=== CREATE USER PROCESSING ===");
        System.out.println("Username: " + user.getUsername());
        System.out.println("Email: " + user.getEmail());
        System.out.println("Full Name: " + user.getFullName());
        System.out.println("Password: " + password);
        System.out.println("Role IDs: " + roleIds);
        
        try {
            // Manual validation
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                model.addAttribute("error", "Username is required!");
                model.addAttribute("allRoles", roleRepository.findAll());
                return "admin/create-user";
            }
            
            if (password == null || password.trim().isEmpty()) {
                model.addAttribute("error", "Password is required!");
                model.addAttribute("allRoles", roleRepository.findAll());
                return "admin/create-user";
            }
            
            if (password.length() < 6) {
                model.addAttribute("error", "Password must be at least 6 characters!");
                model.addAttribute("allRoles", roleRepository.findAll());
                return "admin/create-user";
            }
            
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                model.addAttribute("error", "Email is required!");
                model.addAttribute("allRoles", roleRepository.findAll());
                return "admin/create-user";
            }
            
            // Check if username exists
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                model.addAttribute("error", "Username already exists!");
                model.addAttribute("allRoles", roleRepository.findAll());
                return "admin/create-user";
            }
            
            // Check if email exists
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                model.addAttribute("error", "Email already exists!");
                model.addAttribute("allRoles", roleRepository.findAll());
                return "admin/create-user";
            }
            
            // Set password
            user.setPassword(passwordEncoder.encode(password));
            user.setEnabled(true);
            
            // Set roles
            Set<Role> roles = new HashSet<>();
            if (roleIds != null && !roleIds.isEmpty()) {
                for (Long roleId : roleIds) {
                    roleRepository.findById(roleId).ifPresent(roles::add);
                }
            }
            
            // Default to USER role if no roles selected
            if (roles.isEmpty()) {
                roleRepository.findByName("ROLE_USER").ifPresent(roles::add);
            }
            
            user.setRoles(roles);
            
            // Save user
            User savedUser = userRepository.save(user);
            System.out.println("✓ User created successfully with ID: " + savedUser.getId());
            
            redirectAttributes.addFlashAttribute("success", "User '" + user.getUsername() + "' created successfully!");
            return "redirect:/admin/users";
            
        } catch (Exception e) {
            System.err.println("✗ Error creating user: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error creating user: " + e.getMessage());
            model.addAttribute("allRoles", roleRepository.findAll());
            return "admin/create-user";
        }
    }
    
    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable("id") Long id, Model model) {
        try {
            User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
            
            model.addAttribute("user", user);
            model.addAttribute("allRoles", roleRepository.findAll());
            return "admin/edit-user";
            
        } catch (Exception e) {
            System.err.println("Error loading user: " + e.getMessage());
            return "redirect:/admin/users";
        }
    }
    
    @PostMapping("/users/update/{id}")
    public String updateUser(
            @PathVariable("id") Long id,
            @ModelAttribute("user") User user,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            @RequestParam(value = "roleIds", required = false) List<Long> roleIds,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check if email is being changed and already exists
            if (!existingUser.getEmail().equals(user.getEmail()) &&
                userRepository.findByEmail(user.getEmail()).isPresent()) {
                model.addAttribute("error", "Email already exists!");
                model.addAttribute("allRoles", roleRepository.findAll());
                return "admin/edit-user";
            }
            
            // Update basic info
            existingUser.setFullName(user.getFullName());
            existingUser.setEmail(user.getEmail());
            
            // Update password if provided
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(newPassword));
            }
            
            // Update roles
            Set<Role> roles = new HashSet<>();
            if (roleIds != null && !roleIds.isEmpty()) {
                for (Long roleId : roleIds) {
                    roleRepository.findById(roleId).ifPresent(roles::add);
                }
            }
            existingUser.setRoles(roles);
            
            userRepository.save(existingUser);
            redirectAttributes.addFlashAttribute("success", "User updated successfully!");
            
        } catch (Exception e) {
            System.err.println("Error updating user: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error updating user: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }
    
    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Prevent deleting admin user
            if (user.getUsername().equals("admin")) {
                redirectAttributes.addFlashAttribute("error", "Cannot delete the admin user!");
                return "redirect:/admin/users";
            }
            
            userRepository.delete(user);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully!");
            
        } catch (Exception e) {
            System.err.println("Error deleting user: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error deleting user: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }
    
    @GetMapping("/users/toggle/{id}")
    public String toggleUserStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Prevent disabling admin user
            if (user.getUsername().equals("admin")) {
                redirectAttributes.addFlashAttribute("error", "Cannot disable the admin user!");
                return "redirect:/admin/users";
            }
            
            user.setEnabled(!user.isEnabled());
            userRepository.save(user);
            
            String status = user.isEnabled() ? "enabled" : "disabled";
            redirectAttributes.addFlashAttribute("success", "User " + status + " successfully!");
            
        } catch (Exception e) {
            System.err.println("Error toggling user status: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error toggling user status: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }
    
    // Debug endpoint
    @GetMapping("/debug/users")
    @ResponseBody
    public String debugUsers() {
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>Debug Users</h1>");
        
        sb.append("<h2>All Users:</h2>");
        userRepository.findAll().forEach(user -> {
            sb.append("ID: ").append(user.getId())
              .append(" | Username: ").append(user.getUsername())
              .append(" | Email: ").append(user.getEmail())
              .append(" | Enabled: ").append(user.isEnabled())
              .append(" | Password length: ").append(user.getPassword() != null ? user.getPassword().length() : 0)
              .append("<br>");
        });
        
        sb.append("<h2>All Roles:</h2>");
        roleRepository.findAll().forEach(role -> {
            sb.append("ID: ").append(role.getId())
              .append(" | Name: ").append(role.getName())
              .append("<br>");
        });
        
        return sb.toString();
    }
}