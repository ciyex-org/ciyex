package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.PatientEducationAssignmentDto;
import com.qiaben.ciyex.entity.portal.PortalPatient;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.repository.portal.PortalPatientRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import com.qiaben.ciyex.service.PatientEducationAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalPatientEducationAssignmentService {

    private final PortalUserRepository portalUserRepository;
    private final PortalPatientRepository portalPatientRepository;
    private final PatientEducationAssignmentService assignmentService;

    /**
     * Get patient education assignments for a portal user by email
     * 
     * Resolution chain:
     * 1. email (from JWT) → PortalUser
     * 2. PortalUser → PortalPatient
     * 3. PortalPatient.ehrPatientId → EHR Patient
     * 4. EHR Patient ID → PatientEducationAssignments
     * 
     * This ensures each portal patient sees ONLY their own assignments
     */
    public List<PatientEducationAssignmentDto> getAssignmentsByEmail(String email) {
        try {
            log.info("🔍 Resolving assignments for email: {}", email);

            // Step 1: Find portal user by email
            PortalUser portalUser = portalUserRepository.findByEmail(email)
                    .orElse(null);
            
            if (portalUser == null) {
                log.warn("❌ Portal user not found for email: {}", email);
                return Collections.emptyList();
            }
            log.info("✅ Found PortalUser ID: {}", portalUser.getId());

            // Step 2: Find portal patient linked to this user
            PortalPatient portalPatient = portalPatientRepository.findByPortalUser_Id(portalUser.getId())
                    .orElse(null);
            
            if (portalPatient == null) {
                log.warn("❌ Portal patient not found for PortalUser ID: {}", portalUser.getId());
                return Collections.emptyList();
            }
            log.info("✅ Found PortalPatient ID: {}", portalPatient.getId());

            // Step 3: Get EHR patient ID from portal patient
            Long ehrPatientId = portalPatient.getEhrPatientId();
            if (ehrPatientId == null) {
                log.warn("❌ EHR patient ID not linked for PortalPatient ID: {}", portalPatient.getId());
                log.warn("⚠️  This portal patient needs to be linked to an EHR patient record");
                return Collections.emptyList();
            }
            log.info("✅ Found EHR Patient ID: {}", ehrPatientId);

            // Step 4: Fetch assignments using the EHR patient ID
            log.info("📚 Fetching assignments for EHR Patient ID: {}", ehrPatientId);
            List<PatientEducationAssignmentDto> assignments = assignmentService.getByPatient(ehrPatientId);
            log.info("✅ Found {} assignment(s) for patient", assignments.size());
            
            return assignments;

        } catch (Exception e) {
            log.error("❌ Error fetching patient education assignments for email: {}", email, e);
            return Collections.emptyList();
        }
    }
}
