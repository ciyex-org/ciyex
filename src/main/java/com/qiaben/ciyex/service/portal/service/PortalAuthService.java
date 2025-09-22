package com.qiaben.ciyex.service.portal.service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.qiaben.ciyex.dto.portal.dto.ApiResponse;
import com.qiaben.ciyex.dto.portal.dto.PortalLoginRequest;
import com.qiaben.ciyex.dto.portal.dto.PortalLoginResponse;
import com.qiaben.ciyex.dto.portal.dto.PortalRegisterRequest;
import com.qiaben.ciyex.dto.portal.dto.PortalUserDto;
import com.qiaben.ciyex.entity.Org;
import com.qiaben.ciyex.entity.portal.entity.PortalPatient;
import com.qiaben.ciyex.entity.portal.entity.PortalUser;
import com.qiaben.ciyex.repository.OrgRepository;
import com.qiaben.ciyex.repository.portal.repository.PortalPatientRepository;
import com.qiaben.ciyex.repository.portal.repository.PortalUserRepository;
import com.qiaben.ciyex.util.JwtTokenUtil;   // ✅ using main JwtTokenUtil

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class PortalAuthService {

    private final AuthenticationManager authenticationManager;
    private final PortalUserRepository portalUserRepository;
    private final PortalPatientRepository portalPatientRepository;
    private final OrgRepository orgRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;   // ✅ reuse main util

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    public PortalAuthService(
            AuthenticationManager authenticationManager,
            PortalUserRepository portalUserRepository,
            PortalPatientRepository portalPatientRepository,
            OrgRepository orgRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenUtil jwtTokenUtil
    ) {
        this.authenticationManager = authenticationManager;
        this.portalUserRepository = portalUserRepository;
        this.portalPatientRepository = portalPatientRepository;
        this.orgRepository = orgRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * 🔹 Login using email and password
     */
    public ApiResponse<PortalLoginResponse> login(PortalLoginRequest request) {
    // 1️⃣ Find the portal user
    PortalUser user = portalUserRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found with email: " + request.getEmail()));

    // 2️⃣ Validate password manually
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        return ApiResponse.<PortalLoginResponse>builder()
                .success(false)
                .message("Invalid password")
                .build();
    }

    // 3️⃣ Generate JWT for portal user manually
    String token = generatePortalUserToken(user);

    // 4️⃣ Prepare response
    PortalLoginResponse response = PortalLoginResponse.fromEntity(user);
    response.setToken(token);

    Org org = orgRepository.findById(user.getOrgId()).orElse(null);
    if (org != null) {
        PortalLoginResponse.OrgInfo orgInfo = new PortalLoginResponse.OrgInfo(
                user.getOrgId(),
                org.getOrgName(),
                "PATIENT"
        );
        response.setOrgs(List.of(orgInfo));
    }

    return ApiResponse.<PortalLoginResponse>builder()
            .success(true)
            .message("Login successful")
            .data(response)
            .build();
}

    /**
     * 🔹 Register a new portal user and create linked portal_patient record
     */
    public ApiResponse<PortalUserDto> register(PortalRegisterRequest request) {
        if (portalUserRepository.existsByEmail(request.getEmail())) {
            return ApiResponse.<PortalUserDto>builder()
                    .success(false)
                    .message("Email already in use")
                    .build();
        }

        // 1️⃣ Create the User
        PortalUser user = PortalUser.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .middleName(request.getMiddleName())
                .dateOfBirth(request.getDateOfBirth())
                .phoneNumber(request.getPhoneNumber())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .street(request.getStreet())
                .street2(request.getStreet2())
                .postalCode(request.getPostalCode())
                .profileImage(request.getProfileImage())
                .securityQuestion(request.getSecurityQuestion())
                .securityAnswer(request.getSecurityAnswer())
                .orgId(request.getOrgId() != null ? request.getOrgId() : 1L)
                .uuid(UUID.randomUUID())
                .role("PATIENT")
                .build();

        PortalUser savedUser = portalUserRepository.save(user);

        // 2️⃣ Create the Patient (linked to User)
        PortalPatient patient = PortalPatient.builder()
                .user(savedUser)
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .dob(savedUser.getDateOfBirth())
                .gender(null)
                .phone(savedUser.getPhoneNumber())
                .email(savedUser.getEmail())
                .address(savedUser.getStreet())
                .insuranceId(null)
                .build();

        portalPatientRepository.save(patient);

        // 3️⃣ Prepare DTO
        PortalUserDto dto = PortalUserDto.fromEntity(savedUser);

        Org org = orgRepository.findById(savedUser.getOrgId()).orElse(null);
        if (org != null) {
            dto.setOrgName(org.getOrgName());
        }

        return ApiResponse.<PortalUserDto>builder()
                .success(true)
                .message("User registered successfully")
                .data(dto)
                .build();
    }

    /**
     * 🔹 Generate JWT token specifically for PortalUser entities
     */
    private String generatePortalUserToken(PortalUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("sub", user.getEmail());
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        claims.put("uuid", user.getUuid());

        // For portal users, create a simple org structure
        Map<String, Object> orgEntry = new HashMap<>();
        orgEntry.put("orgId", user.getOrgId());
        orgEntry.put("roles", List.of(user.getRole()));

        // Get org name if available
        Org org = orgRepository.findById(user.getOrgId()).orElse(null);
        if (org != null) {
            orgEntry.put("orgName", org.getOrgName());
        }

        List<Map<String, Object>> orgList = List.of(orgEntry);
        claims.put("orgs", orgList);
        claims.put("orgIds", List.of(user.getOrgId()));

        long nowSec = System.currentTimeMillis() / 1000;
        long expSec = nowSec + (jwtExpirationInMs / 1000);
        claims.put("iat", nowSec);
        claims.put("exp", expSec);

        Key signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date(nowSec * 1000))
                .setExpiration(new Date(expSec * 1000))
                .signWith(signingKey)
                .compact();
    }
}
