package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.UserRegisterRequest;
import com.qiaben.ciyex.model.User;
import com.qiaben.ciyex.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // Register new user - open to all
    @PostMapping("/register")
    public User registerUser(@RequestBody UserRegisterRequest request) {
        return userService.register(request);
    }

    // List all users - ADMIN only
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // Get user by ID - ADMIN only
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    // Update user - ADMIN only
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public User updateUser(@PathVariable Long id, @RequestBody UserRegisterRequest request) {
        return userService.updateUser(id, request);
    }

    // Delete user - ADMIN only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
}
