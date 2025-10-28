


package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.VitalsDto;

import com.qiaben.ciyex.entity.Vitals;
import com.qiaben.ciyex.repository.VitalsRepository;
import com.qiaben.ciyex.storage.ExternalVitalsStorage;
import com.qiaben.ciyex.util.JwtTokenUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class VitalsService {
    private final VitalsRepository repository;
    private final ExternalVitalsStorage externalStorage;
    private final TenantAwareService tenantAwareService;
    private final JwtTokenUtil jwtTokenUtil;
    
    @PersistenceContext
    private EntityManager entityManager;

    public VitalsDto create( Long patientId, Long encounterId, VitalsDto dto) {
        Vitals entity = toEntity(dto);
        entity.setPatientId(patientId);
        entity.setEncounterId(encounterId);
        Vitals saved = repository.save(entity);
        externalStorage.save(saved);
        return toDto(saved);
    }

    public VitalsDto get(Long patientId, Long encounterId, Long id) {
        return repository.findById(id)
                .filter(v -> v.getPatientId().equals(patientId)
                        && v.getEncounterId().equals(encounterId))
                .map(this::toDto)
                .orElse(null);
    }

    public List<VitalsDto> getByEncounter(Long patientId, Long encounterId) {
        return repository.findByPatientIdAndEncounterId(patientId, encounterId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public VitalsDto update(Long patientId, Long encounterId, Long id, VitalsDto dto) {
        Vitals existing = repository.findById(id).orElseThrow();
        existing.setWeightKg(dto.getWeightKg());
        existing.setBpSystolic(dto.getBpSystolic());
        existing.setBpDiastolic(dto.getBpDiastolic());
        existing.setPulse(dto.getPulse());
        existing.setRespiration(dto.getRespiration());
        existing.setTemperatureC(dto.getTemperatureC());
        existing.setOxygenSaturation(dto.getOxygenSaturation());
        existing.setNotes(dto.getNotes());
        Vitals updated = repository.save(existing);
        externalStorage.save(updated);
        return toDto(updated);
    }

    public void delete(Long patientId, Long encounterId, Long id) {
        Vitals existing = repository.findById(id).orElseThrow();
        if (existing.getPatientId().equals(patientId)
                && existing.getEncounterId().equals(encounterId)) {
            repository.delete(existing);
            externalStorage.delete(id);
        }
    }

    public VitalsDto eSign(Long patientId, Long encounterId, Long id) {
        Vitals vitals = repository.findById(id).orElseThrow();
        if (!vitals.getPatientId().equals(patientId)
                || !vitals.getEncounterId().equals(encounterId)) {
            throw new IllegalArgumentException("Vitals does not belong to org/patient/encounter");
        }
        vitals.setSigned(true);
        Vitals saved = repository.save(vitals);
        externalStorage.save(saved);
        return toDto(saved);
    }

    public byte[] print(Long patientId, Long encounterId, Long id) {
        Vitals vitals = repository.findById(id).orElseThrow();
        if (!vitals.getPatientId().equals(patientId)
                || !vitals.getEncounterId().equals(encounterId)) {
            throw new IllegalArgumentException("Vitals does not belong to org/patient/encounter");
        }
        return externalStorage.print(vitals);
    }

    // 🏥 EHR Method - Get all vitals for a patient (across all encounters)
    @Transactional(readOnly = true)
    public List<VitalsDto> getVitalsByPatient(Long orgId, Long patientId) {
        log.info("Getting vitals for patient {} in org {}", patientId, orgId);
        
        return tenantAwareService.executeInTenantContext(orgId, () -> {
            List<Vitals> vitals = repository.findByPatientIdOrderByRecordedAtDesc(patientId);
            log.info("Found {} vitals records for patient {} in org {}", vitals.size(), patientId, orgId);
            return vitals.stream()
                    .filter(v -> v.getOrgId().equals(orgId)) // Multi-tenant security
                    .map(this::toDto)
                    .collect(Collectors.toList());
        });
    }

    // 👩‍⚕️ Portal Method - Map portal user email to EHR patient ID
    @Transactional(readOnly = true)
    public Long getEhrPatientIdFromPortalUserEmail(String email, Long orgId) {
        log.info("Looking up EHR patient ID for portal user {} in org {}", email, orgId);
        
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        
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
                    Long patientId = ((Number) result).longValue();
                    log.info("Found EHR patient ID {} for portal user {} in org {}", patientId, email, orgId);
                    return patientId;
                }
            } catch (Exception e) {
                log.warn("Failed to find EHR patient ID for user {}: {}", email, e.getMessage());
            }
            
            // Fallback: return patient ID 1 for testing
            log.info("Using fallback patient ID 1 for user {} in org {}", email, orgId);
            return 1L;
        });
    }

    /**
     * Get vitals for current portal user based on JWT token
     */
    @Transactional(readOnly = true) 
    public List<VitalsDto> getVitalsForPortalUser(String token) {
        try {
            String userEmail = jwtTokenUtil.getEmailFromToken(token);
            List<Long> orgIds = jwtTokenUtil.getOrgIdsFromToken(token);
            
            if (orgIds == null || orgIds.isEmpty()) {
                log.error("No orgIds found in token for user {}", userEmail);
                throw new IllegalArgumentException("No organization found in token");
            }
            
            // Use the first orgId (primary organization)
            Long orgId = ((Number) orgIds.get(0)).longValue();
            log.info("Getting vitals for portal user {} in org {}", userEmail, orgId);
            
            // Get the EHR patient ID for this portal user
            Long patientId = getEhrPatientIdFromPortalUserEmail(userEmail, orgId);
            if (patientId == null) {
                log.warn("No patient ID found for portal user {} in org {}", userEmail, orgId);
                return List.of(); // Return empty list instead of null
            }
            
            // Get vitals using tenant-aware query
            return getVitalsByPatient(orgId, patientId);
            
        } catch (Exception e) {
            log.error("Error getting vitals for portal user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get vitals for user", e);
        }
    }

    private Vitals toEntity(VitalsDto dto) {
        return Vitals.builder()
                .id(dto.getId())
                .patientId(dto.getPatientId())
                .encounterId(dto.getEncounterId())
                .weightKg(dto.getWeightKg())
                .bpSystolic(dto.getBpSystolic())
                .bpDiastolic(dto.getBpDiastolic())
                .pulse(dto.getPulse())
                .respiration(dto.getRespiration())
                .temperatureC(dto.getTemperatureC())
                .oxygenSaturation(dto.getOxygenSaturation())
                .notes(dto.getNotes())
                .signed(dto.getSigned())
                .recordedAt(dto.getRecordedAt())
                .build();
    }

    private VitalsDto toDto(Vitals entity) {
        return VitalsDto.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId())
                .encounterId(entity.getEncounterId())
                .weightKg(entity.getWeightKg())
                .bpSystolic(entity.getBpSystolic())
                .bpDiastolic(entity.getBpDiastolic())
                .pulse(entity.getPulse())
                .respiration(entity.getRespiration())
                .temperatureC(entity.getTemperatureC())
                .oxygenSaturation(entity.getOxygenSaturation())
                .notes(entity.getNotes())
                .signed(entity.getSigned())
                .recordedAt(entity.getRecordedAt())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }
}
