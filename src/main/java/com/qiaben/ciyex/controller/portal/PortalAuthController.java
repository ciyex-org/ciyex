package com.qiaben.ciyex.controller.portal;

import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalLoginRequest;
import com.qiaben.ciyex.dto.portal.PortalLoginResponse;
import com.qiaben.ciyex.dto.portal.PortalRegisterRequest;
import com.qiaben.ciyex.dto.portal.PortalUserDto;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import com.qiaben.ciyex.service.portal.PortalAuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/portal/auth")
@RequiredArgsConstructor
// 🔹 Enable CORS only for Portal APIs
@CrossOrigin(
    origins = { "http://localhost:3000", "http://127.0.0.1:3000" }, // frontend dev URLs
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS },
    allowCredentials = "true"
)
public class PortalAuthController {

    private final PortalAuthService portalAuthService;
    private final PortalUserRepository portalUserRepository;
    private final PasswordEncoder passwordEncoder;

    /** Patient Portal Registration */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<PortalUserDto>> register(@RequestBody PortalRegisterRequest request) {
        return ResponseEntity.ok(portalAuthService.register(request));
    }

    /** Patient Portal Login — returns JWT in data.token */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<PortalLoginResponse>> login(@RequestBody PortalLoginRequest request) {
        return ResponseEntity.ok(portalAuthService.login(request));
    }

    /** Patient Portal Password Reset */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody PortalLoginRequest request) {
        Optional<PortalUser> userOpt = portalUserRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(false)
                    .message("User not found with email: " + request.getEmail())
                    .build());
        }

        PortalUser user = userOpt.get();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        portalUserRepository.save(user);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Password reset successful")
                .build());
    }

    /** Fetch portal user by ID */
    @GetMapping("/user/{id}")
    public ResponseEntity<ApiResponse<PortalUserDto>> getUserById(@PathVariable Long id) {
        return portalUserRepository.findById(id)
                .map(user -> ApiResponse.<PortalUserDto>builder()
                        .success(true)
                        .message("User found")
                        .data(PortalUserDto.fromEntity(user))
                        .build())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(ApiResponse.<PortalUserDto>builder()
                        .success(false)
                        .message("User not found")
                        .build()));
    }

    /** Fetch portal user by email */
    @GetMapping("/user/email")
    public ResponseEntity<ApiResponse<PortalUserDto>> getUserByEmail(@RequestParam String email) {
        return portalUserRepository.findByEmail(email)
                .map(user -> ApiResponse.<PortalUserDto>builder()
                        .success(true)
                        .message("User found")
                        .data(PortalUserDto.fromEntity(user))
                        .build())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(ApiResponse.<PortalUserDto>builder()
                        .success(false)
                        .message("User not found with email: " + email)
                        .build()));
    }

    /** Update patient profile info */
    @PutMapping("/user/{id}")
    public ResponseEntity<ApiResponse<PortalUserDto>> updateProfile(
            @PathVariable Long id,
            @RequestBody PortalUserDto request) {
        return portalUserRepository.findById(id)
                .map(user -> {
                    user.setFirstName(request.getFirstName());
                    user.setLastName(request.getLastName());
                    user.setPhoneNumber(request.getPhoneNumber());
                    PortalUser updated = portalUserRepository.save(user);

                    return ApiResponse.<PortalUserDto>builder()
                            .success(true)
                            .message("Profile updated successfully")
                            .data(PortalUserDto.fromEntity(updated))
                            .build();
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(ApiResponse.<PortalUserDto>builder()
                        .success(false)
                        .message("User not found")
                        .build()));
    }
}
//