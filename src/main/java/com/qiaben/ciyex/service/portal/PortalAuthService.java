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
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

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
        claims.put("role", "PATIENT"); // This will be picked up by KeycloakJwtAuthenticationConverter
        claims.put("status", user.getStatus().toString());
        
        // ✅ Add preferred_username for compatibility
        claims.put("preferred_username", user.getEmail());
        
        // ✅ Add realm_access structure that Spring Security expects
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", Collections.singletonList("PATIENT"));
        claims.put("realm_access", realmAccess);

        // ✅ Use the secret directly (it's already Base64-encoded in application.yml)
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        
        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION_TIME))
                .signWith(Keys.hmacShaKeyFor(keyBytes))
                .compact();
    }

    /** ✅ Register portal user — auto-approved */
    @Transactional
    public ApiResponse<PortalLoginResponse> register(PortalRegisterRequest request) {
        try {
            if (portalUserRepository.existsByEmail(request.getEmail())) {
                return ApiResponse.<PortalLoginResponse>builder()
                        .success(false)
                        .message("Email already registered")
                        .build();
            }

            // ✅ Auto-approved user
            PortalUser user = PortalUser.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phoneNumber(request.getPhoneNumber())
                    .status(PortalStatus.APPROVED)
                    .approvedDate(LocalDateTime.now())
                    .build();

            PortalUser savedUser = portalUserRepository.save(user);

            // ✅ Create linked portal patient
            PortalPatient patient = PortalPatient.builder()
                    .portalUser(savedUser)
                    .dateOfBirth(request.getDateOfBirth() != null ? request.getDateOfBirth() : LocalDate.now().minusYears(25))
                    .addressLine1(request.getStreet())
                    .addressLine2(request.getStreet2())
                    .city(request.getCity())
                    .state(request.getState())
                    .country(request.getCountry() != null ? request.getCountry() : "USA")
                    .postalCode(request.getPostalCode())
                    .build();

            portalPatientRepository.save(patient);

            log.info("✅ Portal user registered and auto-approved: {}", savedUser.getEmail());

            return ApiResponse.<PortalLoginResponse>builder()
                    .success(true)
                    .message("Registration successful and approved!")
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

    /** ✅ Login for portal user */
    public ApiResponse<PortalLoginResponse> login(PortalLoginRequest request) {
        try {
            Optional<PortalUser> userOpt = portalUserRepository.findByEmail(request.getEmail());
            if (userOpt.isEmpty()) {
                return ApiResponse.<PortalLoginResponse>builder()
                        .success(false)
                        .message("Invalid email or password")
                        .build();
            }

            PortalUser user = userOpt.get();

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ApiResponse.<PortalLoginResponse>builder()
                        .success(false)
                        .message("Invalid email or password")
                        .build();
            }

            if (user.getStatus() != PortalStatus.APPROVED) {
                return ApiResponse.<PortalLoginResponse>builder()
                        .success(false)
                        .message("Your account is not approved yet")
                        .build();
            }

            PortalLoginResponse response = PortalLoginResponse.fromEntity(user);
            if (user.getPortalPatient() != null) {
                PortalPatient patient = user.getPortalPatient();
                response.setDateOfBirth(patient.getDateOfBirth());
                response.setStreet(patient.getAddressLine1());
                response.setCity(patient.getCity());
                response.setState(patient.getState());
                response.setCountry(patient.getCountry());
                response.setPostalCode(patient.getPostalCode());
            }

            response.setToken(generateJwtToken(user));
            log.info("✅ Portal user logged in successfully: {}", user.getEmail());

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

    /** ✅ Validate or auto-create portal user from Keycloak login */
    @Transactional
    public PortalUser ensurePortalUserExistsFromKeycloak(Map<String, Object> userData) {
        String email = (String) userData.getOrDefault("email", "");
        String firstName = (String) userData.getOrDefault("given_name", "Unknown");
        String lastName = (String) userData.getOrDefault("family_name", "");
        String keycloakId = (String) userData.getOrDefault("sub", null);

        if (email.isEmpty()) {
            throw new RuntimeException("Email missing in Keycloak token");
        }

        return portalUserRepository.findByEmail(email).orElseGet(() -> {
            log.info("Creating portal user for new Keycloak user: {}", email);
            PortalUser newUser = PortalUser.builder()
                    .email(email)
                    .password(passwordEncoder.encode("keycloak-login"))
                    .firstName(firstName)
                    .lastName(lastName)
                    .status(PortalStatus.APPROVED)
                    .approvedDate(LocalDateTime.now())
                    .keycloakUserId(keycloakId)
                    .build();
            portalUserRepository.save(newUser);

            PortalPatient newPatient = PortalPatient.builder()
                    .portalUser(newUser)
                    .dateOfBirth(LocalDate.now().minusYears(25))
                    .country("USA")
                    .build();
            portalPatientRepository.save(newPatient);

            return newUser;
        });
    }

    /** ✅ Get user profile by ID */
    public ApiResponse<PortalLoginResponse> getProfile(Long userId) {
        return portalUserRepository.findById(userId)
                .map(user -> ApiResponse.<PortalLoginResponse>builder()
                        .success(true)
                        .message("Profile retrieved successfully")
                        .data(PortalLoginResponse.fromEntity(user))
                        .build())
                .orElse(ApiResponse.<PortalLoginResponse>builder()
                        .success(false)
                        .message("User not found")
                        .build());
    }
}
