package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.MedicationRequestDto;
import com.qiaben.ciyex.entity.MedicationRequest;
import com.qiaben.ciyex.entity.portal.PortalPatient;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.repository.MedicationRequestRepository;
import com.qiaben.ciyex.repository.portal.PortalPatientRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for fetching medication requests linked to a portal user's EHR patient ID.
 * Uses EMAIL as the lookup key, avoiding dependency on Keycloak UUID mapping.
 */
@Service
@Slf4j
public class MedicationsService {

    private final MedicationRequestRepository medicationRequestRepository;
    private final PortalUserRepository portalUserRepository;
    private final PortalPatientRepository portalPatientRepository;

    public MedicationsService(
            MedicationRequestRepository medicationRequestRepository,
            PortalUserRepository portalUserRepository,
            PortalPatientRepository portalPatientRepository
    ) {
        this.medicationRequestRepository = medicationRequestRepository;
        this.portalUserRepository = portalUserRepository;
        this.portalPatientRepository = portalPatientRepository;
    }

    /**
     * Fetch medications for the currently authenticated portal user.
     * Maps user email -> PortalUser -> PortalPatient -> EHR Patient ID.
     */
    public List<MedicationRequestDto> getMedicationsForPortalUser() {

        // 1️⃣ Get Keycloak-authenticated user identity (email)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("❌ No authenticated user found in security context");
            throw new RuntimeException("User not authenticated");
        }

        String email = authentication.getName();
        log.info("🔍 Fetching medications for portal user email: {}", email);

        // 2️⃣ Find portal user by email
        PortalUser portalUser = portalUserRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("❌ Portal user not found for email: {}", email);
                    return new RuntimeException("Portal user not found: " + email);
                });

        // 3️⃣ Get portal patient mapping
        PortalPatient portalPatient = portalPatientRepository.findByPortalUser_Id(portalUser.getId())
                .orElseThrow(() -> {
                    log.error("❌ No portal patient record found for user email: {}", email);
                    return new RuntimeException("Patient mapping not found for user: " + email);
                });

        Long ehrPatientId = portalPatient.getEhrPatientId();
        if (ehrPatientId == null) {
            log.error("❌ Portal patient {} has no EHR patient ID", portalPatient.getId());
            throw new RuntimeException("No EHR patient ID mapped for user: " + email);
        }

        log.info("✅ Email {} maps to EHR patient ID {}", email, ehrPatientId);

        // 4️⃣ Fetch medications for this EHR patient
        List<MedicationRequest> meds = medicationRequestRepository.findByPatientId(ehrPatientId);

        log.info("📦 Found {} medication records for EHR patient {}", meds.size(), ehrPatientId);

        // 5️⃣ Convert entities → DTO
        return meds.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert MedicationRequest entity to DTO.
     */
    private MedicationRequestDto mapToDto(MedicationRequest entity) {
        MedicationRequestDto dto = new MedicationRequestDto();
        dto.setId(entity.getId());
        dto.setPatientId(entity.getPatientId());
        dto.setEncounterId(entity.getEncounterId());
        dto.setMedicationName(entity.getMedicationName());
        dto.setDosage(entity.getDosage());
        dto.setInstructions(entity.getInstructions());
        dto.setDateIssued(entity.getDateIssued());
        dto.setPrescribingDoctor(entity.getPrescribingDoctor());
        dto.setStatus(entity.getStatus());

        MedicationRequestDto.Audit audit = new MedicationRequestDto.Audit();
        dto.setAudit(audit);

        return dto;
    }
}
