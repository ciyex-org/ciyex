package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalUserDto;
import com.qiaben.ciyex.entity.Patient;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.entity.portal.PortalPatient;
import com.qiaben.ciyex.enums.PortalStatus;
import com.qiaben.ciyex.repository.PatientRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import com.qiaben.ciyex.repository.portal.PortalPatientRepository;
import com.qiaben.ciyex.service.TenantProvisionService;
import com.qiaben.ciyex.service.KeycloakUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service to handle portal user approval workflow
 * Users are now managed in Keycloak instead of database
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalApprovalService {

    private final PortalUserRepository portalUserRepository;
    private final PortalPatientRepository portalPatientRepository;
    private final PatientRepository patientRepository;
    private final TenantProvisionService tenantProvisionService;
    private final KeycloakUserService keycloakUserService;

    /**
     * Get all pending portal users waiting for approval
     */
    public ApiResponse<List<PortalUserDto>> getPendingUsers() {
        try {
            List<PortalUser> pendingUsers = portalUserRepository.findPendingUsers();
            List<PortalUserDto> userDtos = pendingUsers.stream()
                    .map(PortalUserDto::fromEntity)
                    .collect(Collectors.toList());

            return ApiResponse.<List<PortalUserDto>>builder()
                    .success(true)
                    .message("Pending users retrieved successfully")
                    .data(userDtos)
                    .build();
        } catch (Exception e) {
            log.error("Error retrieving pending users", e);
            return ApiResponse.<List<PortalUserDto>>builder()
                    .success(false)
                    .message("Failed to retrieve pending users")
                    .build();
        }
    }

    /**
     * Get pending portal users for a specific organization
     */
    public ApiResponse<List<PortalUserDto>> getPendingUsersByOrg(Long orgId) {
        try {
            List<PortalUser> pendingUsers = portalUserRepository.findPendingUsersByOrgId(orgId);
            List<PortalUserDto> userDtos = pendingUsers.stream()
                    .map(PortalUserDto::fromEntity)
                    .collect(Collectors.toList());

            return ApiResponse.<List<PortalUserDto>>builder()
                    .success(true)
                    .message("Pending users retrieved successfully")
                    .data(userDtos)
                    .build();
        } catch (Exception e) {
            log.error("Error retrieving pending users for org: {}", orgId, e);
            return ApiResponse.<List<PortalUserDto>>builder()
                    .success(false)
                    .message("Failed to retrieve pending users")
                    .build();
        }
    }

    /**
     * Approve a portal user and create corresponding tenant user/patient
     */
    @Transactional
    public ApiResponse<PortalUserDto> approveUser(Long portalUserId, Long approvedByUserId) {
        try {
            PortalUser portalUser = portalUserRepository.findById(portalUserId)
                    .orElse(null);

            if (portalUser == null) {
                return ApiResponse.<PortalUserDto>builder()
                        .success(false)
                        .message("Portal user not found")
                        .build();
            }

            if (portalUser.getStatus() != PortalStatus.PENDING) {
                return ApiResponse.<PortalUserDto>builder()
                        .success(false)
                        .message("User is not in pending status")
                        .build();
            }

            // Get the associated portal patient
            PortalPatient portalPatient = portalPatientRepository.findByPortalUser_Id(portalUserId)
                    .orElse(null);

            if (portalPatient == null) {
                return ApiResponse.<PortalUserDto>builder()
                        .success(false)
                        .message("Portal patient not found")
                        .build();
            }

            // Create tenant user and patient
            Long ehrPatientId = createTenantPatient(portalUser, portalPatient);

            // Update portal user status
            portalUser.setStatus(PortalStatus.APPROVED);
            portalUser.setApprovedDate(LocalDateTime.now());
            portalUser.setApprovedBy(approvedByUserId);
            portalUser.setLastModifiedDate(LocalDateTime.now());

            // Update portal patient with EHR patient ID
            portalPatient.setEhrPatientId(ehrPatientId);
            portalPatient.setLastModifiedDate(LocalDateTime.now());

            // Save changes
            portalUserRepository.save(portalUser);
            portalPatientRepository.save(portalPatient);

            log.info("Portal user approved successfully: {} -> EHR Patient ID: {}", 
                    portalUser.getEmail(), ehrPatientId);

            return ApiResponse.<PortalUserDto>builder()
                    .success(true)
                    .message("Portal user approved and synced to EHR tenant")
                    .data(PortalUserDto.fromEntity(portalUser))
                    .build();

        } catch (Exception e) {
            log.error("Error approving portal user: {}", portalUserId, e);
            return ApiResponse.<PortalUserDto>builder()
                    .success(false)
                    .message("Failed to approve user: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Reject a portal user
     */
    @Transactional
    public ApiResponse<PortalUserDto> rejectUser(Long portalUserId, String reason, Long rejectedByUserId) {
        try {
            PortalUser portalUser = portalUserRepository.findById(portalUserId)
                    .orElse(null);

            if (portalUser == null) {
                return ApiResponse.<PortalUserDto>builder()
                        .success(false)
                        .message("Portal user not found")
                        .build();
            }

            if (portalUser.getStatus() != PortalStatus.PENDING) {
                return ApiResponse.<PortalUserDto>builder()
                        .success(false)
                        .message("User is not in pending status")
                        .build();
            }

            // Update portal user status
            portalUser.setStatus(PortalStatus.REJECTED);
            portalUser.setReason(reason);
            portalUser.setRejectedDate(LocalDateTime.now());
            portalUser.setRejectedBy(rejectedByUserId);
            portalUser.setLastModifiedDate(LocalDateTime.now());

            portalUserRepository.save(portalUser);

            log.info("Portal user rejected: {} - Reason: {}", portalUser.getEmail(), reason);

            return ApiResponse.<PortalUserDto>builder()
                    .success(true)
                    .message("Portal user rejected successfully")
                    .data(PortalUserDto.fromEntity(portalUser))
                    .build();

        } catch (Exception e) {
            log.error("Error rejecting portal user: {}", portalUserId, e);
            return ApiResponse.<PortalUserDto>builder()
                    .success(false)
                    .message("Failed to reject user: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Create tenant user in Keycloak and patient in database from portal data
     */
    private Long createTenantPatient(PortalUser portalUser, PortalPatient portalPatient) {
        try {
            // Prepare user attributes for Keycloak
            Map<String, String> attributes = new HashMap<>();
            attributes.put("uuid", portalUser.getUuid().toString());
            attributes.put("dateOfBirth", portalPatient.getDateOfBirth().toString());
            attributes.put("phoneNumber", portalUser.getPhoneNumber());
            attributes.put("street", portalPatient.getAddressLine1());
            if (portalPatient.getAddressLine2() != null) {
                attributes.put("street2", portalPatient.getAddressLine2());
            }
            attributes.put("city", portalPatient.getCity());
            attributes.put("state", portalPatient.getState());
            attributes.put("postalCode", portalPatient.getPostalCode());
            attributes.put("country", portalPatient.getCountry());
            attributes.put("orgId", String.valueOf(portalUser.getOrgId()));
            
            // Create user in Keycloak
            String keycloakUserId = keycloakUserService.createUser(
                    portalUser.getEmail(),
                    portalUser.getFirstName(),
                    portalUser.getLastName(),
                    portalUser.getPassword(), // Already encoded
                    attributes
            );
            
            // Add user to tenant group
            String tenantGroup = "/Tenants/practice_" + portalUser.getOrgId();
            keycloakUserService.addUserToGroup(keycloakUserId, tenantGroup);
            
            // Assign patient role
            keycloakUserService.assignRolesToUser(keycloakUserId, List.of("patient"));
            
            // Create tenant patient in database
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

            log.info("Created Keycloak user {} and patient {} for portal user {}", 
                    keycloakUserId, savedPatient.getId(), portalUser.getEmail());

            return savedPatient.getId();

        } catch (Exception e) {
            log.error("Error creating tenant patient for portal user: {}", portalUser.getEmail(), e);
            throw new RuntimeException("Failed to create tenant patient", e);
        }
    }
}