package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.MedicationRequestDto;
import com.qiaben.ciyex.entity.MedicationRequest;
import com.qiaben.ciyex.repository.MedicationRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MedicationsService {

    private final MedicationRequestRepository medicationRequestRepository;

    public MedicationsService(MedicationRequestRepository medicationRequestRepository) {
        this.medicationRequestRepository = medicationRequestRepository;
    }

    public List<MedicationRequestDto> getMedicationsForPortalUser() {
        // Get Keycloak user from authentication context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authenticated user found in security context");
            throw new RuntimeException("User not authenticated");
        }

        // Get Keycloak user UUID
        String keycloakUserId = authentication.getName();
        log.info("Processing medication request for Keycloak user: {}", keycloakUserId);

        // Get EHR patient ID from portal mapping using repository
        Long ehrPatientId = medicationRequestRepository.findEhrPatientIdByKeycloakUserId(keycloakUserId);
        if (ehrPatientId == null) {
            log.error("No patient mapping found for Keycloak user: {}", keycloakUserId);
            throw new RuntimeException("Patient mapping not found for user: " + keycloakUserId);
        }

        log.info("Found EHR patient ID {} for Keycloak user {}", ehrPatientId, keycloakUserId);

        // Query medications for this patient
        List<MedicationRequest> medications = medicationRequestRepository.findByPatientId(ehrPatientId);
        return medications.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }


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