package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ChangePasswordRequest;
import org.ciyex.ehr.dto.UpdateAddressRequest;
import org.ciyex.ehr.dto.UpdateUserProfileRequest;
import org.ciyex.ehr.service.KeycloakUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

/**
 * User management controller using Keycloak
 * Users are now managed in Keycloak instead of database
 */
@Slf4j
@PreAuthorize("hasAuthority('SCOPE_user/Practitioner.read')")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final KeycloakUserService keycloakUserService;

    @PutMapping("/email/{email}/profile")
    @PreAuthorize("hasAuthority('SCOPE_user/Practitioner.write')")
    public ResponseEntity<?> updateProfileByEmail(@PathVariable String email, @RequestBody UpdateUserProfileRequest request) {
        try {
            // Update user profile in Keycloak
            // Note: This would require finding user by email first, then updating
            log.info("Update profile request for email: {}", email);
            
            // TODO: Implement Keycloak user update by email
            // 1. Search for user by email
            // 2. Update user attributes
            
            return ResponseEntity.ok().body(Map.of(
                "message", "Profile update not yet implemented for Keycloak users",
                "note", "Users are now managed in Keycloak"
            ));
        } catch (Exception e) {
            log.error("Error updating profile for email: {}", email, e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @PutMapping("/user/address")
    @PreAuthorize("hasAuthority('SCOPE_user/Practitioner.write')")
    public ResponseEntity<?> updateAddress(@RequestBody UpdateAddressRequest request) {
        try {
            log.info("Update address request for email: {}", request.getEmail());
            
            // TODO: Implement Keycloak user address update
            // 1. Search for user by email
            // 2. Update address attributes
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Address update not yet implemented for Keycloak users",
                "note", "Users are now managed in Keycloak"
            ));
        } catch (Exception e) {
            log.error("Error updating address", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasAuthority('SCOPE_user/Practitioner.write')")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            log.info("Change password request for email: {}", request.getEmail());
            
            // TODO: Implement Keycloak password change
            // 1. Search for user by email
            // 2. Update password credentials
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Password change not yet implemented for Keycloak users",
                "note", "Users should change password through Keycloak account management"
            ));
        } catch (Exception e) {
            log.error("Error changing password", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
