package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ChangePasswordRequest;
import com.qiaben.ciyex.dto.UpdateAddressRequest;
import com.qiaben.ciyex.dto.UpdateUserProfileRequest;
import com.qiaben.ciyex.security.RequireScope;
import com.qiaben.ciyex.service.UserService; // this is now a concrete class
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequireScope("user:read")  // Default scope for user operations
public class UserController {

    @Autowired
    private UserService userService; // No interface anymore

    @PutMapping("/email/{email}/profile")
    @RequireScope("user:write")
    public ResponseEntity<?> updateProfileByEmail(@PathVariable String email, @RequestBody UpdateUserProfileRequest request) {
        try {
            userService.updateUserProfileByEmail(email, request);
            return ResponseEntity.ok().body(Map.of("message", "Profile updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @PutMapping("/user/address")
    @RequireScope("user:write")
    public ResponseEntity<?> updateAddress(@RequestBody UpdateAddressRequest request) {
        try {
            userService.updateUserAddress(request.getEmail(), request);
            return ResponseEntity.ok(Map.of("success", true, "message", "Address updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    @RequireScope("user:write")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            userService.changeUserPassword(request);
            return ResponseEntity.ok(Map.of("success", true, "message", "Password updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }



}
