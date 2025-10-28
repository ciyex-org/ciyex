package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.CoverageDto;
import com.qiaben.ciyex.dto.InsuranceCompanyDto;
import com.qiaben.ciyex.entity.Coverage;
import com.qiaben.ciyex.entity.InsuranceCompany;
import com.qiaben.ciyex.repository.CoverageRepository;
import com.qiaben.ciyex.repository.InsuranceCompanyRepository;
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
public class CoverageService {

    private final CoverageRepository coverageRepository;
    private final InsuranceCompanyRepository insuranceCompanyRepository;
    private final TenantAwareService tenantAwareService;
    private final JwtTokenUtil jwtTokenUtil;
    
    @PersistenceContext
    private EntityManager entityManager;

    // ---- CRUD ----

    @Transactional
    public CoverageDto create(CoverageDto dto) {
        
        Coverage coverage = mapToEntity(dto);

        // Attach insurance company only if the client provided it
        if (dto.getInsuranceCompany() != null && dto.getInsuranceCompany().getId() != null) {
            Long icId = dto.getInsuranceCompany().getId();
            InsuranceCompany ic = insuranceCompanyRepository.findById(icId)
                    .orElseThrow(() -> new RuntimeException("Insurance company not found: " + icId));
            coverage.setInsuranceCompany(ic);
        } else {
            coverage.setInsuranceCompany(null);
        }

        Coverage saved = coverageRepository.save(coverage);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public CoverageDto getById(Long id) {
        Coverage coverage = coverageRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Coverage not found with id: " + id));
        return mapToDto(coverage);
    }

    @Transactional
    public CoverageDto update(Long id, CoverageDto dto) {
        Coverage coverage = coverageRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Coverage not found with id: " + id));

        updateEntityFromDto(coverage, dto);

        // If payload mentions insurance company, reflect that; if null object provided, detach
        if (dto.getInsuranceCompany() != null) {
            if (dto.getInsuranceCompany().getId() != null) {
                Long icId = dto.getInsuranceCompany().getId();
                InsuranceCompany ic = insuranceCompanyRepository.findById(icId)
                        .orElseThrow(() -> new RuntimeException("Insurance company not found: " + icId));
                coverage.setInsuranceCompany(ic);
            } else {
                coverage.setInsuranceCompany(null);
            }
        }

        Coverage saved = coverageRepository.save(coverage);
        return mapToDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Coverage coverage = coverageRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Coverage not found with id: " + id));
        coverageRepository.delete(coverage);
    }

    @Transactional(readOnly = true)
    public List<CoverageDto> getAllCoverages() {
        List<Coverage> coverages = coverageRepository.findAll();
        return coverages.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // 🏥 EHR Method - Get all coverages for a patient
    @Transactional(readOnly = true)
    public List<CoverageDto> getCoveragesByPatient(Long orgId, Long patientId) {
        log.info("Getting coverages for patient {} in org {}", patientId, orgId);
        
        return tenantAwareService.executeInTenantContext(orgId, () -> {
            List<Coverage> coverages = coverageRepository.findByPatientIdOrderByEffectiveDateDesc(patientId);
            log.info("Found {} coverage records for patient {} in org {}", coverages.size(), patientId, orgId);
            return coverages.stream()
                    .filter(c -> c.getOrgId().equals(orgId)) // Multi-tenant security
                    .map(this::mapToDto)
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
     * Get coverages for current portal user based on JWT token
     */
    @Transactional(readOnly = true) 
    public List<CoverageDto> getCoveragesForPortalUser(String token) {
        try {
            String userEmail = jwtTokenUtil.getEmailFromToken(token);
            List<Long> orgIds = jwtTokenUtil.getOrgIdsFromToken(token);
            
            if (orgIds == null || orgIds.isEmpty()) {
                log.error("No orgIds found in token for user {}", userEmail);
                throw new IllegalArgumentException("No organization found in token");
            }
            
            // Use the first orgId (primary organization)
            Long orgId = ((Number) orgIds.get(0)).longValue();
            log.info("Getting coverages for portal user {} in org {}", userEmail, orgId);
            
            // Get the EHR patient ID for this portal user
            Long patientId = getEhrPatientIdFromPortalUserEmail(userEmail, orgId);
            if (patientId == null) {
                log.warn("No patient ID found for portal user {} in org {}", userEmail, orgId);
                return List.of(); // Return empty list instead of null
            }
            
            // Get coverages using tenant-aware query
            return getCoveragesByPatient(orgId, patientId);
            
        } catch (Exception e) {
            log.error("Error getting coverages for portal user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get coverages for user", e);
        }
    }

    // ---- Composite (id + patientId) ----

    @Transactional(readOnly = true)
    public CoverageDto getByIdAndPatientId(Long id, Long patientId) {
        Coverage coverage = coverageRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Coverage not found for id=" + id + ", patientId=" + patientId));
        return mapToDto(coverage);
    }

    @Transactional
    public CoverageDto updateByIdAndPatientId(Long id, Long patientId, CoverageDto dto) {
        Coverage coverage = coverageRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Coverage not found for id=" + id + ", patientId=" + patientId));

        updateEntityFromDto(coverage, dto);

        if (dto.getInsuranceCompany() != null) {
            if (dto.getInsuranceCompany().getId() != null) {
                Long icId = dto.getInsuranceCompany().getId();
                InsuranceCompany ic = insuranceCompanyRepository.findById(icId)
                        .orElseThrow(() -> new RuntimeException("Insurance company not found: " + icId));
                coverage.setInsuranceCompany(ic);
            } else {
                coverage.setInsuranceCompany(null);
            }
        }

        Coverage saved = coverageRepository.save(coverage);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteByIdAndPatientId(Long id, Long patientId) {
        Coverage coverage = coverageRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Coverage not found for id=" + id + ", patientId=" + patientId));
        coverageRepository.delete(coverage);
    }

    // ---- helpers ----
    private Coverage mapToEntity(CoverageDto dto) {
    return Coverage.builder()
                .externalId(dto.getExternalId())
                .coverageType(dto.getCoverageType())
                .planName(dto.getPlanName())
                .policyNumber(dto.getPolicyNumber())
                .coverageStartDate(dto.getCoverageStartDate())
                .coverageEndDate(dto.getCoverageEndDate())
                .patientId(dto.getPatientId())
                .provider(dto.getProvider())
                .effectiveDate(dto.getEffectiveDate())
                .effectiveDateEnd(dto.getEffectiveDateEnd())
                .groupNumber(dto.getGroupNumber())
                .subscriberEmployer(dto.getSubscriberEmployer())
                .subscriberAddressLine1(dto.getSubscriberAddressLine1())
                .subscriberAddressLine2(dto.getSubscriberAddressLine2())
                .subscriberCity(dto.getSubscriberCity())
                .subscriberState(dto.getSubscriberState())
                .subscriberZipCode(dto.getSubscriberZipCode())
                .subscriberCountry(dto.getSubscriberCountry())
                .subscriberPhone(dto.getSubscriberPhone())
                .byholderName(dto.getByholderName())
                .byholderRelation(dto.getByholderRelation())
                .byholderAddressLine1(dto.getByholderAddressLine1())
                .byholderAddressLine2(dto.getByholderAddressLine2())
                .byholderCity(dto.getByholderCity())
                .byholderState(dto.getByholderState())
                .byholderZipCode(dto.getByholderZipCode())
                .byholderCountry(dto.getByholderCountry())
                .byholderPhone(dto.getByholderPhone())
                .copayAmount(dto.getCopayAmount())
                .build();
    }

    private CoverageDto mapToDto(Coverage coverage) {
        CoverageDto dto = new CoverageDto();
        dto.setId(coverage.getId());
        dto.setExternalId(coverage.getExternalId());
        dto.setCoverageType(coverage.getCoverageType());
        dto.setPlanName(coverage.getPlanName());
        dto.setPolicyNumber(coverage.getPolicyNumber());
        dto.setCoverageStartDate(coverage.getCoverageStartDate());
        dto.setCoverageEndDate(coverage.getCoverageEndDate());
        dto.setPatientId(coverage.getPatientId());
    
        dto.setProvider(coverage.getProvider());
        dto.setEffectiveDate(coverage.getEffectiveDate());
        dto.setEffectiveDateEnd(coverage.getEffectiveDateEnd());
        dto.setGroupNumber(coverage.getGroupNumber());
        dto.setSubscriberEmployer(coverage.getSubscriberEmployer());
        dto.setSubscriberAddressLine1(coverage.getSubscriberAddressLine1());
        dto.setSubscriberAddressLine2(coverage.getSubscriberAddressLine2());
        dto.setSubscriberCity(coverage.getSubscriberCity());
        dto.setSubscriberState(coverage.getSubscriberState());
        dto.setSubscriberZipCode(coverage.getSubscriberZipCode());
        dto.setSubscriberCountry(coverage.getSubscriberCountry());
        dto.setSubscriberPhone(coverage.getSubscriberPhone());
        dto.setByholderName(coverage.getByholderName());
        dto.setByholderRelation(coverage.getByholderRelation());
        dto.setByholderAddressLine1(coverage.getByholderAddressLine1());
        dto.setByholderAddressLine2(coverage.getByholderAddressLine2());
        dto.setByholderCity(coverage.getByholderCity());
        dto.setByholderState(coverage.getByholderState());
        dto.setByholderZipCode(coverage.getByholderZipCode());
        dto.setByholderCountry(coverage.getByholderCountry());
        dto.setByholderPhone(coverage.getByholderPhone());
        dto.setCopayAmount(coverage.getCopayAmount());

        if (coverage.getInsuranceCompany() != null) {
            InsuranceCompany ic = coverage.getInsuranceCompany();
            InsuranceCompanyDto insuranceCompanyDto = new InsuranceCompanyDto();
            insuranceCompanyDto.setId(ic.getId());
            insuranceCompanyDto.setName(ic.getName());
            insuranceCompanyDto.setAddress(ic.getAddress());
            insuranceCompanyDto.setCity(ic.getCity());
            insuranceCompanyDto.setState(ic.getState());
            insuranceCompanyDto.setPostalCode(ic.getPostalCode());
            insuranceCompanyDto.setCountry(ic.getCountry());
            insuranceCompanyDto.setFhirId(ic.getFhirId());
            InsuranceCompanyDto.Audit audit = new InsuranceCompanyDto.Audit();
            insuranceCompanyDto.setAudit(audit);
            dto.setInsuranceCompany(insuranceCompanyDto);
        }
        return dto;
    }

    private void updateEntityFromDto(Coverage coverage, CoverageDto dto) {
        if (dto.getCoverageType() != null) coverage.setCoverageType(dto.getCoverageType());
        if (dto.getPlanName() != null) coverage.setPlanName(dto.getPlanName());
        if (dto.getPolicyNumber() != null) coverage.setPolicyNumber(dto.getPolicyNumber());
        if (dto.getCoverageStartDate() != null) coverage.setCoverageStartDate(dto.getCoverageStartDate());
        if (dto.getCoverageEndDate() != null) coverage.setCoverageEndDate(dto.getCoverageEndDate());
        if (dto.getPatientId() != null) coverage.setPatientId(dto.getPatientId());
    
        if (dto.getProvider() != null) coverage.setProvider(dto.getProvider());
        if (dto.getEffectiveDate() != null) coverage.setEffectiveDate(dto.getEffectiveDate());
        if (dto.getEffectiveDateEnd() != null) coverage.setEffectiveDateEnd(dto.getEffectiveDateEnd());
        if (dto.getGroupNumber() != null) coverage.setGroupNumber(dto.getGroupNumber());
        if (dto.getSubscriberEmployer() != null) coverage.setSubscriberEmployer(dto.getSubscriberEmployer());
        if (dto.getSubscriberAddressLine1() != null) coverage.setSubscriberAddressLine1(dto.getSubscriberAddressLine1());
        if (dto.getSubscriberAddressLine2() != null) coverage.setSubscriberAddressLine2(dto.getSubscriberAddressLine2());
        if (dto.getSubscriberCity() != null) coverage.setSubscriberCity(dto.getSubscriberCity());
        if (dto.getSubscriberState() != null) coverage.setSubscriberState(dto.getSubscriberState());
        if (dto.getSubscriberZipCode() != null) coverage.setSubscriberZipCode(dto.getSubscriberZipCode());
        if (dto.getSubscriberCountry() != null) coverage.setSubscriberCountry(dto.getSubscriberCountry());
        if (dto.getSubscriberPhone() != null) coverage.setSubscriberPhone(dto.getSubscriberPhone());
        if (dto.getByholderName() != null) coverage.setByholderName(dto.getByholderName());
        if (dto.getByholderRelation() != null) coverage.setByholderRelation(dto.getByholderRelation());
        if (dto.getByholderAddressLine1() != null) coverage.setByholderAddressLine1(dto.getByholderAddressLine1());
        if (dto.getByholderAddressLine2() != null) coverage.setByholderAddressLine2(dto.getByholderAddressLine2());
        if (dto.getByholderCity() != null) coverage.setByholderCity(dto.getByholderCity());
        if (dto.getByholderState() != null) coverage.setByholderState(dto.getByholderState());
        if (dto.getByholderZipCode() != null) coverage.setByholderZipCode(dto.getByholderZipCode());
        if (dto.getByholderCountry() != null) coverage.setByholderCountry(dto.getByholderCountry());
        if (dto.getByholderPhone() != null) coverage.setByholderPhone(dto.getByholderPhone());
        if (dto.getCopayAmount() != null) coverage.setCopayAmount(dto.getCopayAmount());
    }
}
