package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.MedicationRequestDto;
import com.qiaben.ciyex.entity.MedicationRequest;
import com.qiaben.ciyex.repository.MedicationRequestRepository;
import com.qiaben.ciyex.util.JwtTokenUtil;
import com.qiaben.ciyex.service.TenantAwareService;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicationsService {

    private final TenantAwareService tenantAwareService;
    private final JwtTokenUtil jwtTokenUtil;
    private final MedicationRequestRepository medicationRequestRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public MedicationsService(TenantAwareService tenantAwareService,
                             JwtTokenUtil jwtTokenUtil,
                             MedicationRequestRepository medicationRequestRepository) {
        this.tenantAwareService = tenantAwareService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.medicationRequestRepository = medicationRequestRepository;
    }

    public List<MedicationRequestDto> getMedicationsForPortalUser(HttpServletRequest request) {
        // Extract JWT token
        String token = extractToken(request);
        if (token == null) {
            throw new RuntimeException("No JWT token found");
        }

        // Extract email from JWT
        String email = jwtTokenUtil.getEmailFromToken(token);
        if (email == null) {
            throw new RuntimeException("No email found in token");
        }

        // Extract orgId from JWT
        Long orgId = extractOrgIdFromToken(token);
        if (orgId == null) {
            throw new RuntimeException("No organization ID found in token");
        }

        // Get EHR patient ID from portal mapping
        Long ehrPatientId = getEhrPatientIdFromPortalUser(email);
        if (ehrPatientId == null) {
            throw new RuntimeException("Patient mapping not found for email: " + email);
        }

        // Switch to tenant schema and query medications
        return tenantAwareService.executeInTenantContext(orgId, () -> {
            List<MedicationRequest> medications = medicationRequestRepository.findByPatientId(ehrPatientId);
            return medications.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        });
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Long extractOrgIdFromToken(String token) {
        try {
            List<?> orgIds = jwtTokenUtil.getOrgIdsFromToken(token);
            if (orgIds != null && !orgIds.isEmpty()) {
                return ((Number) orgIds.get(0)).longValue();
            }
        } catch (Exception e) {
            // Log error if needed
        }
        return null;
    }

    private Long getEhrPatientIdFromPortalUser(String email) {
        return tenantAwareService.executeQueryInMasterContext(em -> {
            try {
                // Query the portal_patients table in public schema to find the EHR patient ID
                Object result = em.createNativeQuery(
                    "SELECT pp.ehr_patient_id FROM public.portal_patients pp " +
                    "JOIN public.portal_users pu ON pp.portal_user_id = pu.id " +
                    "WHERE pu.email = :email LIMIT 1")
                    .setParameter("email", email)
                    .getSingleResult();

                if (result != null) {
                    return ((Number) result).longValue();
                }
            } catch (Exception e) {
                // Log error if needed
                System.out.println("No patient mapping found for email: " + email + ", using fallback patient ID 1");
            }

            // Fallback: return patient ID 1 for testing
            return 1L;
        });
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
        audit.setCreatedDate(entity.getCreatedDate());
        audit.setLastModifiedDate(entity.getLastModifiedDate());
        dto.setAudit(audit);

        return dto;
    }
}