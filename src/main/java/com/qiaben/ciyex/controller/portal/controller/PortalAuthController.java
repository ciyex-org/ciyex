package com.qiaben.ciyex.controller.portal.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qiaben.ciyex.dto.portal.dto.ApiResponse;
import com.qiaben.ciyex.dto.portal.dto.PortalLoginRequest;
import com.qiaben.ciyex.dto.portal.dto.PortalLoginResponse;
import com.qiaben.ciyex.dto.portal.dto.PortalRegisterRequest;
import com.qiaben.ciyex.dto.portal.dto.PortalUserDto;
import com.qiaben.ciyex.entity.portal.entity.PortalUser;
import com.qiaben.ciyex.repository.portal.repository.PortalUserRepository;
import com.qiaben.ciyex.service.portal.service.PortalAuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/portal/auth")
@RequiredArgsConstructor
public class PortalAuthController {

    private final PortalAuthService portalAuthService;
    private final PortalUserRepository portalUserRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<PortalUserDto>> register(@RequestBody PortalRegisterRequest request) {
        return ResponseEntity.ok(portalAuthService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<PortalLoginResponse>> login(@RequestBody PortalLoginRequest request) {
        return ResponseEntity.ok(portalAuthService.login(request));
    }

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

    @PutMapping("/user/{id}")
    public ResponseEntity<ApiResponse<PortalUserDto>> updateProfile(@PathVariable Long id,
                                                                    @RequestBody PortalUserDto request) {
        return portalUserRepository.findById(id)
                .map(user -> {
                    user.setFirstName(request.getFirstName());
                    user.setLastName(request.getLastName());
                    user.setPhoneNumber(request.getPhoneNumber());
                    user.setCity(request.getCity());
                    user.setState(request.getState());
                    user.setStreet(request.getStreet());
                    user.setStreet2(request.getStreet2());
                    user.setPostalCode(request.getPostalCode());
                    user.setCountry(request.getCountry());
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
