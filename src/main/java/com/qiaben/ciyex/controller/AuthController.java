package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.entity.RoleName;
import com.qiaben.ciyex.entity.User;
import com.qiaben.ciyex.service.CiyexUserDetailsService;
import com.qiaben.ciyex.util.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private CiyexUserDetailsService ciyexUserDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody User loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

            Optional<User> userOptional = ciyexUserDetailsService.getUserByEmail(loginRequest.getEmail());

            if (userOptional.isPresent()) {
                User user = userOptional.get();

                String token = jwtTokenUtil.generateToken(user);

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("token", token);
                responseData.put("userId", user.getId());
                responseData.put("firstName", user.getFirstName());
                responseData.put("LastName", user.getLastName());
                responseData.put("phone", user.getPhoneNumber());
                responseData.put("dateOfBirth", user.getDateOfBirth());
                responseData.put("uuid", user.getUuid());
                responseData.put("email", user.getEmail());
                responseData.put("street", user.getStreet());
                responseData.put("street2", user.getStreet2());
                responseData.put("city", user.getCity());
                responseData.put("state", user.getState());
                responseData.put("postalCode", user.getPostalCode());
                responseData.put("country", user.getCountry());


                responseData.put("securityQuestion", user.getSecurityQuestion());
                responseData.put("securityAnswer", user.getSecurityAnswer());

                responseData.put("orgs", jwtTokenUtil.getOrgsFromToken(token));
                responseData.put("orgIds", jwtTokenUtil.getOrgIdsFromToken(token));

                return ResponseEntity.ok(
                        ApiResponse.<Map<String, Object>>builder()
                                .success(true)
                                .message("Login successful")
                                .data(responseData)
                                .build()
                );
            } else {
                log.warn("User not found with email: " + loginRequest.getEmail());
                return ResponseEntity.status(400).body(
                        ApiResponse.<Map<String, Object>>builder()
                                .success(false)
                                .message("User not found")
                                .data(null)
                                .build()
                );
            }
        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: " + loginRequest.getEmail() + ". Exception: " + e.getMessage(), e);
            return ResponseEntity.status(401).body(
                    ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Invalid credentials")
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            log.error("An error occurred during login for user: " + loginRequest.getEmail() + ". Exception: " + e.getMessage());
            return ResponseEntity.status(500).body(
                    ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("An error occurred while processing the login request")
                            .data(null)
                            .build()
            );
        }
    }


    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(
            @RequestBody User user,
            @RequestParam Long orgId
    ) {
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            User savedUser = ciyexUserDetailsService.assignUserToOrg(user, orgId, RoleName.PATIENT);
            log.info("User registered successfully with email/org: " + savedUser.getEmail() + "/" + orgId);
            savedUser.setPassword(null);
            return ResponseEntity.ok(
                    ApiResponse.<User>builder()
                            .success(true)
                            .message("User registered and assigned to org.")
                            .data(savedUser)
                            .build()
            );
        } catch (Exception e) {
            log.error("An error occurred while registering user: " + user.getEmail() + ". Exception: " + e.getMessage());
            return ResponseEntity.status(500).body(
                    ApiResponse.<User>builder()
                            .success(false)
                            .message("An error occurred while processing the registration request.")
                            .data(null)
                            .build()
            );
        }
    }


    @GetMapping("/encode-password/{rawPassword}")
    public ResponseEntity<ApiResponse<String>> encodePassword(@PathVariable String rawPassword) {
        try {
            String encodedPassword = passwordEncoder.encode(rawPassword);
            log.info("Password encoded successfully.");
            return ResponseEntity.ok(
                    ApiResponse.<String>builder()
                            .success(true)
                            .message("Password encoded successfully.")
                            .data(encodedPassword)
                            .build()
            );
        } catch (Exception e) {
            log.error("An error occurred while encoding the password. Exception: " + e.getMessage());
            return ResponseEntity.status(500).body(
                    ApiResponse.<String>builder()
                            .success(false)
                            .message("An error occurred while encoding the password.")
                            .data(null)
                            .build()
            );
        }
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        try {
            List<User> users = ciyexUserDetailsService.getAllUsers();
            log.info("Successfully retrieved " + users.size() + " users.");
            return ResponseEntity.ok(
                    ApiResponse.<List<User>>builder()
                            .success(true)
                            .message("Successfully retrieved users.")
                            .data(users)
                            .build()
            );
        } catch (Exception e) {
            log.error("An error occurred while retrieving users. Exception: " + e.getMessage());
            return ResponseEntity.status(500).body(
                    ApiResponse.<List<User>>builder()
                            .success(false)
                            .message("An error occurred while retrieving users.")
                            .data(null)
                            .build()
            );
        }
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<ApiResponse<User>> getUserByEmail(@PathVariable String email) {
        try {
            Optional<User> userOptional = ciyexUserDetailsService.getUserByEmail(email);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                log.info("User found with email: " + email);
                return ResponseEntity.ok(
                        ApiResponse.<User>builder()
                                .success(true)
                                .message("User retrieved successfully.")
                                .data(user)
                                .build()
                );
            } else {
                log.warn("User not found with email: " + email);
                return ResponseEntity.status(404).body(
                        ApiResponse.<User>builder()
                                .success(false)
                                .message("User not found")
                                .data(null)
                                .build()
                );
            }
        } catch (Exception e) {
            log.error("An error occurred while retrieving user with email: " + email + ". Exception: " + e.getMessage());
            return ResponseEntity.status(500).body(
                    ApiResponse.<User>builder()
                            .success(false)
                            .message("An error occurred while retrieving the user.")
                            .data(null)
                            .build()
            );
        }
    }

    // UPDATE (email + facility assignment)
    @PutMapping("/user/{email}")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @PathVariable String email,
            @RequestBody User user
    ) {
        try {
            User existingUser = ciyexUserDetailsService.getUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!existingUser.getEmail().equals(user.getEmail())) {
                Optional<User> userWithSameEmail = ciyexUserDetailsService.getUserByEmail(user.getEmail());
                if (userWithSameEmail.isPresent()) {
                    log.warn("Attempt to update to an existing email: " + user.getEmail());
                    return ResponseEntity.status(400).body(
                            ApiResponse.<User>builder()
                                    .success(false)
                                    .message("User with this email already exists.")
                                    .data(null)
                                    .build()
                    );
                }
                existingUser.setEmail(user.getEmail());
            }

            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
            }

            existingUser.setFirstName(user.getFirstName());
            existingUser.setMiddleName(user.getMiddleName());
            existingUser.setLastName(user.getLastName());
            existingUser.setUuid(user.getUuid());
            existingUser.setPhoneNumber(user.getPhoneNumber());
            existingUser.setCity(user.getCity());
            existingUser.setState(user.getState());
            existingUser.setStreet(user.getStreet());
            existingUser.setPostalCode(user.getPostalCode());
            existingUser.setCountry(user.getCountry());
            existingUser.setProfileImage(user.getProfileImage());
            existingUser.setDateOfBirth(user.getDateOfBirth());
            existingUser.setSecurityQuestion(user.getSecurityQuestion());
            existingUser.setSecurityAnswer(user.getSecurityAnswer());

            User updatedUser = ciyexUserDetailsService.updateUserByEmail(existingUser.getEmail(), existingUser);
            updatedUser.setPassword(null);

            log.info("User updated successfully with email: " + updatedUser.getEmail());

            return ResponseEntity.ok(
                    ApiResponse.<User>builder()
                            .success(true)
                            .message("User updated successfully.")
                            .data(updatedUser)
                            .build()
            );
        } catch (RuntimeException e) {
            log.warn("Error: " + e.getMessage());
            return ResponseEntity.status(404).body(
                    ApiResponse.<User>builder()
                            .success(false)
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            log.error("An error occurred while updating user with email: " + email + ". Exception: " + e.getMessage());
            return ResponseEntity.status(500).body(
                    ApiResponse.<User>builder()
                            .success(false)
                            .message("An error occurred while updating the user.")
                            .data(null)
                            .build()
            );
        }
    }

    @DeleteMapping("/user/{email}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String email) {
        try {
            Optional<User> userOpt = ciyexUserDetailsService.getUserByEmail(email);
            if (userOpt.isEmpty()) {
                log.warn("User not found with email: " + email);
                return ResponseEntity.status(404).body(
                        ApiResponse.<Void>builder()
                                .success(false)
                                .message("User not found with email: " + email)
                                .data(null)
                                .build()
                );
            }
            ciyexUserDetailsService.deleteUserByEmail(email);
            log.info("User deleted successfully with email: " + email);

            return ResponseEntity.ok(
                    ApiResponse.<Void>builder()
                            .success(true)
                            .message("User deleted successfully.")
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            log.error("An error occurred while deleting user with email: " + email + ". Exception: " + e.getMessage());
            return ResponseEntity.status(500).body(
                    ApiResponse.<Void>builder()
                            .success(false)
                            .message("An error occurred while deleting the user.")
                            .data(null)
                            .build()
            );
        }
    }

    // Password reset (no orgId required anymore)
    public static class PasswordResetRequest {
        private String email;
        private String newPassword;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    @PostMapping("/user/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody PasswordResetRequest resetRequest) {
        try {
            Optional<User> userOptional = ciyexUserDetailsService.getUserByEmail(resetRequest.getEmail());

            if (userOptional.isEmpty()) {
                log.warn("User not found with email: " + resetRequest.getEmail());
                return ResponseEntity.badRequest().body(
                        ApiResponse.<Void>builder()
                                .success(false)
                                .message("User not found")
                                .data(null)
                                .build()
                );
            }

            User user = userOptional.get();
            user.setPassword(passwordEncoder.encode(resetRequest.getNewPassword()));
            ciyexUserDetailsService.updateUserByEmail(user.getEmail(), user);

            log.info("Password updated successfully for email: " + user.getEmail());

            return ResponseEntity.ok(
                    ApiResponse.<Void>builder()
                            .success(true)
                            .message("Password updated successfully")
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            log.error("An error occurred while resetting password for email: " + resetRequest.getEmail() + ". Exception: " + e.getMessage());
            return ResponseEntity.status(500).body(
                    ApiResponse.<Void>builder()
                            .success(false)
                            .message("An error occurred while updating the password.")
                            .data(null)
                            .build()
            );
        }
    }
}
