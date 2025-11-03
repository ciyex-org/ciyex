package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalLoginRequest;
import com.qiaben.ciyex.dto.portal.PortalLoginResponse;
import com.qiaben.ciyex.dto.portal.PortalRegisterRequest;
import com.qiaben.ciyex.entity.portal.PortalPatient;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.enums.PortalStatus;
import com.qiaben.ciyex.repository.portal.PortalPatientRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.jsonwebtoken.Jwts;

/**
 * Service for portal user authentication and registration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalAuthService {

    private final PortalUserRepository portalUserRepository;
    private final PortalPatientRepository portalPatientRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.secret:portal-secret-key-for-development-only}")
    private String jwtSecret;

    private static final long JWT_EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 24 hours

    private String generateJwtToken(PortalUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", "PORTAL_USER");
        claims.put("status", user.getStatus().toString());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION_TIME))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(Base64.getEncoder().encodeToString(jwtSecret.getBytes()).getBytes()))
                .compact();
    }

    /**
     * Register a new portal user
     */
    @Transactional
    public ApiResponse<PortalLoginResponse> register(PortalRegisterRequest request) {
        try {
            // Check if email already exists
            if (portalUserRepository.existsByEmail(request.getEmail())) {
                return ApiResponse.<PortalLoginResponse>builder()
                    .success(false)
                    .message("Email already registered")
                    .build();
            }

            // Create portal user
            PortalUser user = PortalUser.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .status(PortalStatus.PENDING)
                .build();

            PortalUser savedUser = portalUserRepository.save(user);

            // Create portal patient with additional details
            PortalPatient patient = PortalPatient.builder()
                .portalUser(savedUser)
                .dateOfBirth(request.getDateOfBirth())
                .addressLine1(request.getStreet())
                .addressLine2(request.getStreet2())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry() != null ? request.getCountry() : "USA")
                .postalCode(request.getPostalCode())
                .build();

            portalPatientRepository.save(patient);

            log.info("Portal user registered successfully: {}", savedUser.getEmail());

            return ApiResponse.<PortalLoginResponse>builder()
                .success(true)
                .message("Registration successful! Please wait for EHR approval before logging in.")
                .data(PortalLoginResponse.fromEntity(savedUser))
                .build();

        } catch (Exception e) {
            log.error("Registration failed", e);
            return ApiResponse.<PortalLoginResponse>builder()
                .success(false)
                .message("Registration failed: " + e.getMessage())
                .build();
        }
    }

    /**
     * Authenticate portal user login
     */
    public ApiResponse<PortalLoginResponse> login(PortalLoginRequest request) {
        try {
            // Find user by email
            Optional<PortalUser> userOpt = portalUserRepository.findByEmail(request.getEmail());
            if (userOpt.isEmpty()) {
                return ApiResponse.<PortalLoginResponse>builder()
                    .success(false)
                    .message("Invalid email or password")
                    .build();
            }

            PortalUser user = userOpt.get();

            // Check password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ApiResponse.<PortalLoginResponse>builder()
                    .success(false)
                    .message("Invalid email or password")
                    .build();
            }

            // Check if user is approved
            if (user.getStatus() != PortalStatus.APPROVED) {
                String message = switch (user.getStatus()) {
                    case PENDING -> "Your account is pending approval. Please wait for EHR staff to review your registration.";
                    case REJECTED -> "Your registration was rejected. Reason: " + (user.getReason() != null ? user.getReason() : "No reason provided");
                    default -> "Account status unknown";
                };

                return ApiResponse.<PortalLoginResponse>builder()
                    .success(false)
                    .message(message)
                    .build();
            }

            // Create login response
            PortalLoginResponse response = PortalLoginResponse.fromEntity(user);

            // Populate additional patient data
            if (user.getPortalPatient() != null) {
                PortalPatient patient = user.getPortalPatient();
                response.setDateOfBirth(patient.getDateOfBirth());
                response.setStreet(patient.getAddressLine1());
                response.setStreet2(patient.getAddressLine2());
                response.setCity(patient.getCity());
                response.setState(patient.getState());
                response.setCountry(patient.getCountry());
                response.setPostalCode(patient.getPostalCode());
            }

            // Generate JWT token
            response.setToken(generateJwtToken(user));

            log.info("Portal user logged in successfully: {}", user.getEmail());

            return ApiResponse.<PortalLoginResponse>builder()
                .success(true)
                .message("Login successful")
                .data(response)
                .build();

        } catch (Exception e) {
            log.error("Login failed", e);
            return ApiResponse.<PortalLoginResponse>builder()
                .success(false)
                .message("Login failed: " + e.getMessage())
                .build();
        }
    }

    /**
     * Get user profile by ID
     */
    public ApiResponse<PortalLoginResponse> getProfile(Long userId) {
        try {
            Optional<PortalUser> userOpt = portalUserRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ApiResponse.<PortalLoginResponse>builder()
                    .success(false)
                    .message("User not found")
                    .build();
            }

            PortalUser user = userOpt.get();
            PortalLoginResponse response = PortalLoginResponse.fromEntity(user);

            // Populate additional patient data
            if (user.getPortalPatient() != null) {
                PortalPatient patient = user.getPortalPatient();
                response.setDateOfBirth(patient.getDateOfBirth());
                response.setStreet(patient.getAddressLine1());
                response.setStreet2(patient.getAddressLine2());
                response.setCity(patient.getCity());
                response.setState(patient.getState());
                response.setCountry(patient.getCountry());
                response.setPostalCode(patient.getPostalCode());
            }

            return ApiResponse.<PortalLoginResponse>builder()
                .success(true)
                .message("Profile retrieved successfully")
                .data(response)
                .build();

        } catch (Exception e) {
            log.error("Failed to get profile", e);
            return ApiResponse.<PortalLoginResponse>builder()
                .success(false)
                .message("Failed to get profile: " + e.getMessage())
                .build();
        }
    }
}