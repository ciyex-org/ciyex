package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalLoginRequest;
import com.qiaben.ciyex.dto.portal.PortalLoginResponse;
import com.qiaben.ciyex.dto.portal.PortalRegisterRequest;
import com.qiaben.ciyex.dto.portal.PortalUserDto;
import com.qiaben.ciyex.entity.User;
import com.qiaben.ciyex.entity.Patient;
import com.qiaben.ciyex.entity.Org;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.entity.portal.PortalPatient;
import com.qiaben.ciyex.enums.PortalStatus;
import com.qiaben.ciyex.repository.OrgRepository;
import com.qiaben.ciyex.repository.UserRepository;
import com.qiaben.ciyex.repository.PatientRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import com.qiaben.ciyex.repository.portal.PortalPatientRepository;
import com.qiaben.ciyex.service.TenantProvisionService;
import com.qiaben.ciyex.util.JwtTokenUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Date;


@Slf4j
@Service
@RequiredArgsConstructor
public class PortalAuthService {

    private final PortalUserRepository portalUserRepository;
    private final PortalPatientRepository portalPatientRepository;
    private final OrgRepository orgRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final TenantProvisionService tenantProvisionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Portal Login – generates token only if user is APPROVED
     */
    public ApiResponse<PortalLoginResponse> login(PortalLoginRequest request) {
        try {
            // 1️⃣ Lookup portal user
            PortalUser portalUser = portalUserRepository.findByEmail(request.getEmail())
                    .orElse(null);

            if (portalUser == null) {
                return ApiResponse.<PortalLoginResponse>builder()
                        .success(false)
                        .message("Invalid email or password")
                        .build();
            }

            // 2️⃣ Validate password
            if (!passwordEncoder.matches(request.getPassword(), portalUser.getPassword())) {
                return ApiResponse.<PortalLoginResponse>builder()
                        .success(false)
                        .message("Invalid email or password")
                        .build();
            }

            // 3️⃣ Check rejection status (only block rejected users)
            if (portalUser.getStatus() == PortalStatus.REJECTED) {
                String reason = portalUser.getReason() != null ? portalUser.getReason() : "Contact administrator";
                return ApiResponse.<PortalLoginResponse>builder()
                        .success(false)
                        .message("Your registration was rejected. Reason: " + reason)
                        .build();
            }

            // 4️⃣ Generate JWT for approved users with orgId
            String token = generatePortalUserToken(portalUser);

            // 5️⃣ Build response DTO with org information
            PortalLoginResponse response = PortalLoginResponse.fromEntity(portalUser);
            response.setToken(token);
            
            // Add org information to response
            Long userOrgId = portalUser.getOrgId() != null ? portalUser.getOrgId() : 3L;
            String orgName = "Portal Org";
            try {
                Org org = orgRepository.findById(userOrgId).orElse(null);
                if (org != null) {
                    orgName = org.getOrgName();
                }
            } catch (Exception e) {
                log.warn("Could not fetch org name for orgId: {}", userOrgId, e);
            }
            
            List<PortalLoginResponse.OrgInfo> orgsList = new ArrayList<>();
            orgsList.add(new PortalLoginResponse.OrgInfo(userOrgId, orgName, "PATIENT"));
            response.setOrgs(orgsList);

            return ApiResponse.<PortalLoginResponse>builder()
                    .success(true)
                    .message("Login successful")
                    .data(response)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error during portal login for email: {}", request.getEmail(), e);
            return ApiResponse.<PortalLoginResponse>builder()
                    .success(false)
                    .message("Login failed. Please try again.")
                    .build();
        }
    }

    /**
     * Portal Register – create portal user + linked portal patient with PENDING status
     */
    @Transactional
    public ApiResponse<PortalUserDto> register(PortalRegisterRequest request) {
        try {
            if (portalUserRepository.existsByEmail(request.getEmail())) {
                return ApiResponse.<PortalUserDto>builder()
                        .success(false)
                        .message("Email already in use")
                        .build();
            }

            // 1️⃣ Build portal user with APPROVED status for immediate login
            PortalUser user = PortalUser.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phoneNumber(request.getPhoneNumber())
                    .status(PortalStatus.APPROVED)  // ✅ IMMEDIATE LOGIN ALLOWED
                    .orgId(request.getOrgId() != null ? request.getOrgId() : 3L)
                    .createdDate(LocalDateTime.now())
                    .lastModifiedDate(LocalDateTime.now())
                    .build();

            // 2️⃣ Save user first
            PortalUser savedUser = portalUserRepository.save(user);

            // 3️⃣ Build patient and link to saved user
            PortalPatient patient = PortalPatient.builder()
                    .portalUser(savedUser)
                    .dateOfBirth(request.getDateOfBirth())
                    .addressLine1(request.getStreet())
                    .addressLine2(request.getStreet2())
                    .city(request.getCity())
                    .state(request.getState())
                    .postalCode(request.getPostalCode())
                    .country(request.getCountry() != null ? request.getCountry() : "USA")
                    .createdDate(LocalDateTime.now())
                    .lastModifiedDate(LocalDateTime.now())
                    .build();

            // 4️⃣ Save patient
            portalPatientRepository.save(patient);

            // 5️⃣ Create EHR patient and link it
            Long ehrPatientId = createEhrPatient(savedUser, patient);
            patient.setEhrPatientId(ehrPatientId);
            portalPatientRepository.save(patient);

            // 6️⃣ Build DTO response
            PortalUserDto dto = PortalUserDto.fromEntity(savedUser);
            Org org = orgRepository.findById(savedUser.getOrgId()).orElse(null);
            if (org != null) dto.setOrgName(org.getOrgName());

            log.info("Portal user registered and linked to EHR patient {}: {}", ehrPatientId, savedUser.getEmail());

            return ApiResponse.<PortalUserDto>builder()
                    .success(true)
                    .message("Registration successful! You can now login and access your portal.")
                    .data(dto)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error during portal registration for email: {}", request.getEmail(), e);
            return ApiResponse.<PortalUserDto>builder()
                    .success(false)
                    .message("Registration failed. Please try again.")
                    .build();
        }
    }

    /**
     * Generate JWT token specifically for portal users with proper orgId setup
     */
    private String generatePortalUserToken(PortalUser portalUser) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", portalUser.getId());
        claims.put("email", portalUser.getEmail());
        claims.put("sub", portalUser.getEmail());
        claims.put("firstName", portalUser.getFirstName());
        claims.put("lastName", portalUser.getLastName());
        claims.put("uuid", portalUser.getUuid());

        // Set up org information for portal users
        List<Long> orgIds = new ArrayList<>();
        List<Map<String, Object>> orgs = new ArrayList<>();
        
        // Determine orgId for token: prefer portalUser.orgId -> fallback to DB org -> final default 3L
        Long chosenOrgId = null;
        if (portalUser.getOrgId() != null) {
            chosenOrgId = portalUser.getOrgId();
        } else {
            try {
                if (portalUser.getEmail() != null) {
                    // Try to find portal user from repository to get orgId (defensive)
                    PortalUser persisted = portalUserRepository.findByEmail(portalUser.getEmail()).orElse(null);
                    if (persisted != null && persisted.getOrgId() != null) {
                        chosenOrgId = persisted.getOrgId();
                    }
                }
            } catch (Exception e) {
                log.warn("Could not lookup portal user for org fallback", e);
            }
        }

        if (chosenOrgId == null) {
            // Try to use first available org in master table as a last resort, else default to 3L
            try {
                Optional<Org> firstOrg = orgRepository.findAll().stream().findFirst();
                if (firstOrg.isPresent()) {
                    chosenOrgId = firstOrg.get().getId();
                }
            } catch (Exception e) {
                log.debug("Failed to lookup first org from OrgRepository", e);
            }
        }

        if (chosenOrgId == null) chosenOrgId = 3L;

        orgIds.add(chosenOrgId);
        Map<String, Object> orgEntry = new HashMap<>();
        orgEntry.put("orgId", chosenOrgId);
        orgEntry.put("orgName", "Portal Org");
        orgEntry.put("roles", Arrays.asList("ROLE_PATIENT"));
        orgs.add(orgEntry);
        log.info("Generating portal token for user {} with chosenOrgId={}", portalUser.getEmail(), chosenOrgId);
        
        claims.put("orgs", orgs);
        claims.put("orgIds", orgIds);
        claims.put("roles", Arrays.asList("ROLE_PATIENT"));

        long nowSec = System.currentTimeMillis() / 1000;
        long expSec = nowSec + (3600 * 24); // 24 hours expiration
        claims.put("iat", nowSec);
        claims.put("exp", expSec);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(portalUser.getEmail())
                .setIssuedAt(new Date(nowSec * 1000))
                .setExpiration(new Date(expSec * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Create EHR patient and link to portal patient during registration
     */
    private Long createEhrPatient(PortalUser portalUser, PortalPatient portalPatient) {
        try {
            // Create tenant user
            User tenantUser = User.builder()
                    .uuid(portalUser.getUuid())
                    .email(portalUser.getEmail())
                    .password(portalUser.getPassword()) // Already encoded
                    .firstName(portalUser.getFirstName())
                    .lastName(portalUser.getLastName())
                    .dateOfBirth(portalPatient.getDateOfBirth())
                    .phoneNumber(portalUser.getPhoneNumber())
                    .street(portalPatient.getAddressLine1())
                    .street2(portalPatient.getAddressLine2())
                    .city(portalPatient.getCity())
                    .state(portalPatient.getState())
                    .postalCode(portalPatient.getPostalCode())
                    .country(portalPatient.getCountry())
                    .build();

            User savedUser = userRepository.save(tenantUser);

            // Create tenant patient
            Patient tenantPatient = Patient.builder()
                    .orgId(portalUser.getOrgId())
                    .firstName(portalUser.getFirstName())
                    .lastName(portalUser.getLastName())
                    .email(portalUser.getEmail())
                    .phoneNumber(portalUser.getPhoneNumber())
                    .dateOfBirth(portalPatient.getDateOfBirth().toString())
                    .gender(portalPatient.getGender())
                    .address(portalPatient.getAddressLine1())
                    .status("ACTIVE")
                    .createdDate(LocalDateTime.now().toString())
                    .lastModifiedDate(LocalDateTime.now().toString())
                    .build();

            Patient savedPatient = patientRepository.save(tenantPatient);

            log.info("Created EHR user {} and patient {} for portal user {}", 
                    savedUser.getId(), savedPatient.getId(), portalUser.getEmail());

            return savedPatient.getId();

        } catch (Exception e) {
            log.error("Error creating EHR patient for portal user: {}", portalUser.getEmail(), e);
            throw new RuntimeException("Failed to create EHR patient", e);
        }
    }
}
//