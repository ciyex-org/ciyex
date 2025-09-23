package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalLoginRequest;
import com.qiaben.ciyex.dto.portal.PortalLoginResponse;
import com.qiaben.ciyex.dto.portal.PortalRegisterRequest;
import com.qiaben.ciyex.dto.portal.PortalUserDto;
import com.qiaben.ciyex.entity.*;
import com.qiaben.ciyex.entity.portal.PortalPatient;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.repository.OrgRepository;
import com.qiaben.ciyex.repository.portal.PortalPatientRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import com.qiaben.ciyex.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PortalAuthService {

    private final PortalUserRepository portalUserRepository;
    private final PortalPatientRepository portalPatientRepository;
    private final OrgRepository orgRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * 🔹 Portal Login – generates token using main JwtTokenUtil
     */
    public ApiResponse<PortalLoginResponse> login(PortalLoginRequest request) {
        // 1️⃣ Lookup portal user
        PortalUser portalUser = portalUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Portal user not found with email: " + request.getEmail()));

        // 2️⃣ Validate password
        if (!passwordEncoder.matches(request.getPassword(), portalUser.getPassword())) {
            return ApiResponse.<PortalLoginResponse>builder()
                    .success(false)
                    .message("Invalid password")
                    .build();
        }

        // 3️⃣ Wrap PortalUser into synthetic User for JwtTokenUtil
        User tempUser = User.builder()
                .id(portalUser.getId())
                .uuid(portalUser.getUuid())
                .email(portalUser.getEmail())
                .firstName(portalUser.getFirstName())
                .lastName(portalUser.getLastName())
                .password(portalUser.getPassword())
                .build();

        Org org = orgRepository.findById(portalUser.getOrgId())
                .orElseThrow(() -> new RuntimeException("Org not found with id: " + portalUser.getOrgId()));

        UserOrgRole role = UserOrgRole.builder()
                .user(tempUser)
                .org(org)
                .role(RoleName.PATIENT)   // ✅ assign portal role
                .build();

        tempUser.getUserOrgRoles().add(role);

        // 4️⃣ Generate JWT
        String token = jwtTokenUtil.generateToken(tempUser);

        // 5️⃣ Build response DTO
        PortalLoginResponse response = PortalLoginResponse.fromEntity(portalUser);
        response.setToken(token);

        response.setOrgs(List.of(new PortalLoginResponse.OrgInfo(
                portalUser.getOrgId(),
                org.getOrgName(),
                role.getRole().name()
        )));

        return ApiResponse.<PortalLoginResponse>builder()
                .success(true)
                .message("Portal login successful")
                .data(response)
                .build();
    }

    /**
     * 🔹 Portal Register – create portal user + linked portal patient
     */
    /**
     * 🔹 Portal Register – create portal user + linked portal patient
     */
    public ApiResponse<PortalUserDto> register(PortalRegisterRequest request) {
        if (portalUserRepository.existsByEmail(request.getEmail())) {
            return ApiResponse.<PortalUserDto>builder()
                    .success(false)
                    .message("Email already in use")
                    .build();
        }

        // 1️⃣ Build user
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

        // 2️⃣ Build patient and link
        PortalPatient patient = PortalPatient.builder()
                .user(user)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dob(request.getDateOfBirth())
                .gender(null)
                .phone(request.getPhoneNumber())
                .email(request.getEmail())
                .address(request.getStreet())
                .insuranceId(null)
                .build();

        user.setPatient(patient);

        // 3️⃣ Save user (cascades patient automatically)
        PortalUser savedUser = portalUserRepository.save(user);

        // 4️⃣ Build DTO
        PortalUserDto dto = PortalUserDto.fromEntity(savedUser);
        Org org = orgRepository.findById(savedUser.getOrgId()).orElse(null);
        if (org != null) dto.setOrgName(org.getOrgName());

        return ApiResponse.<PortalUserDto>builder()
                .success(true)
                .message("User registered successfully")
                .data(dto)
                .build();
    }
}