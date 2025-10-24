package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PatientDto;
import com.qiaben.ciyex.entity.Patient;
import com.qiaben.ciyex.repository.PatientRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PatientService {

    private final PatientRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    @Autowired
    public PatientService(PatientRepository repository,
                          ExternalStorageResolver storageResolver,
                          OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    @Transactional(readOnly = true)
    public long countPatientsForCurrentOrg() {
        Long orgId = getCurrentOrgId();
        if (orgId == null) {
            log.error("No orgId found in RequestContext during count");
            throw new SecurityException("No orgId available in request context");
        }
        log.info("Counting patients for orgId: {}", orgId);
        return repository.countByOrgId(orgId);
    }

    // Create a new patient
    @Transactional
    public PatientDto create(PatientDto dto) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during create");
            throw new SecurityException("No orgId available in request context");
        }
        dto.setTenantName(currentTenantName());

        if (dto.getFirstName() == null || dto.getLastName() == null) {
            throw new IllegalArgumentException("First name and last name are required");
        }

        // ✅ Auto-generate MRN if missing
        if (dto.getMedicalRecordNumber() == null || dto.getMedicalRecordNumber().isBlank()) {
            dto.setMedicalRecordNumber(generateMrn());
        }

        Patient patient = mapToEntity(dto);
        patient.setOrgId(currentOrgId);
        patient.setCreatedDate(LocalDateTime.now().toString());
        patient.setLastModifiedDate(LocalDateTime.now().toString());

        String externalId = null;
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<PatientDto> externalStorage = storageResolver.resolve(PatientDto.class);
                externalId = externalStorage.create(dto);
                log.info("Successfully created patient in external storage with externalId: {} for orgId: {}", externalId, currentOrgId);
            } catch (Exception e) {
                log.error("Failed to create patient in external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        patient.setExternalId(externalId);

        // Save within tenant schema
        patient = repository.save(patient);
        
        if (patient.getId() == null) {
            log.error("Database save failed to generate id for patient with externalId: {} and orgId: {}", externalId, currentOrgId);
            throw new RuntimeException("Failed to generate id for new patient");
        }

    dto.setId(patient.getId());
    dto.setExternalId(externalId);
    dto.setTenantName(currentTenantName());
        log.info("Created patient with id: {} and externalId: {} in DB for orgId: {}", patient.getId(), externalId, currentOrgId);

        return dto;
    }

    // Fetch patient by ID
    @Transactional(readOnly = true)
    public PatientDto getById(Long id) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during getById for id: {}", id);
            throw new SecurityException("No orgId available in request context");
        }

        Patient patient = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + id));

        if (!currentOrgId.equals(patient.getOrgId())) {
            throw new SecurityException("Access denied: Patient id " + id + " does not belong to orgId " + currentOrgId);
        }

        PatientDto patientDto = mapToDto(patient);
        if (patient.getExternalId() != null) {
            PatientDto fhirPatientDto = getPatientFromFhir(patient.getExternalId());
            if (fhirPatientDto != null) {
                patientDto.setPreferredName(fhirPatientDto.getPreferredName());
                patientDto.setLicenseId(fhirPatientDto.getLicenseId());
                patientDto.setSexualOrientation(fhirPatientDto.getSexualOrientation());
                patientDto.setEmergencyContact(fhirPatientDto.getEmergencyContact());
                patientDto.setRace(fhirPatientDto.getRace());
                patientDto.setEthnicity(fhirPatientDto.getEthnicity());
                patientDto.setGuardianName(fhirPatientDto.getGuardianName());
                patientDto.setGuardianRelationship(fhirPatientDto.getGuardianRelationship());
            }
        }

        return patientDto;
    }

    // Fetch extended FHIR data for a patient
    public PatientDto getPatientFromFhir(String externalId) {
        if (externalId == null) return null;
        ExternalStorage<PatientDto> externalStorage = storageResolver.resolve(PatientDto.class);
        return externalStorage.get(externalId);
    }

    // Update an existing patient
    @Transactional
    public PatientDto update(Long id, PatientDto dto) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during update for id: {}", id);
            throw new SecurityException("No orgId available in request context");
        }

        Patient patient = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + id));

        if (!currentOrgId.equals(patient.getOrgId())) {
            throw new SecurityException("Access denied: Patient id " + id + " does not belong to orgId " + currentOrgId);
        }

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && patient.getExternalId() != null) {
            try {
                ExternalStorage<PatientDto> externalStorage = storageResolver.resolve(PatientDto.class);
                externalStorage.update(dto, patient.getExternalId());
                log.info("Successfully updated patient with id: {} and externalId: {} in external storage for orgId: {}", id, patient.getExternalId(), currentOrgId);
            } catch (Exception e) {
                log.error("Failed to update patient in external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        updateEntityFromDto(patient, dto);
        patient.setLastModifiedDate(LocalDateTime.now().toString());
        patient = repository.save(patient);

    dto.setId(patient.getId());
    dto.setExternalId(patient.getExternalId());
    dto.setTenantName(currentTenantName());
        log.info("Updated patient with id: {} and externalId: {} in DB for orgId: {}", id, patient.getExternalId(), currentOrgId);

        return dto;
    }

    // Delete a patient
    @Transactional
    public void delete(Long id) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during delete for id: {}", id);
            throw new SecurityException("No orgId available in request context");
        }

        Patient patient = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + id));

        if (!currentOrgId.equals(patient.getOrgId())) {
            throw new SecurityException("Access denied: Patient id " + id + " does not belong to orgId " + currentOrgId);
        }

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && patient.getExternalId() != null) {
            try {
                ExternalStorage<PatientDto> externalStorage = storageResolver.resolve(PatientDto.class);
                externalStorage.delete(patient.getExternalId());
            } catch (Exception e) {
                log.error("Failed to delete patient from external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        repository.delete(patient);
        log.info("Deleted patient with id: {} from DB for orgId: {}", id, currentOrgId);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<PatientDto>> getAllPatients() {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            return ApiResponse.<List<PatientDto>>builder()
                    .success(false)
                    .message("No orgId available in request context")
                    .build();
        }

    List<Patient> patients = repository.findAllByOrgId(currentOrgId);
    List<PatientDto> patientDtos = patients.stream().map(this::mapToDto).collect(Collectors.toList());

        return ApiResponse.<List<PatientDto>>builder()
                .success(true)
                .message("Patients retrieved successfully")
                .data(patientDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<PatientDto> searchPatients(String query, Pageable pageable) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during patient search");
            throw new SecurityException("No orgId available in request context");
        }

        if (query == null || query.isBlank()) {
            log.info("Empty search query provided, returning all patients for orgId: {}", currentOrgId);
            return repository.findAllByOrgId(currentOrgId, pageable).map(this::mapToDto);
        }

        log.info("Searching patients for orgId: {} with query: {}", currentOrgId, query);
        return repository.searchByOrgId(query.toLowerCase(), currentOrgId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<PatientDto> getAllPatients(Pageable pageable, String search) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during paginated retrieval");
            throw new SecurityException("No orgId available in request context");
        }

        Page<Patient> page;
        if (search != null && !search.isBlank()) {
            // TODO: Add tenant-aware search method
            page = repository.searchByOrgId(search.toLowerCase(), currentOrgId, pageable);
        } else {
            page = repository.findAllByOrgId(currentOrgId, pageable);
        }
        return page.map(this::mapToDto);
    }

    // --- Helper methods ---

    private Patient mapToEntity(PatientDto dto) {
        Patient patient = new Patient();
        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());
        patient.setMiddleName(dto.getMiddleName());
        patient.setGender(dto.getGender());
        patient.setDateOfBirth(dto.getDateOfBirth());
        patient.setPhoneNumber(dto.getPhoneNumber());
        patient.setEmail(dto.getEmail());
        patient.setAddress(dto.getAddress());
        patient.setStatus(dto.getStatus() != null ? dto.getStatus() : "Active");

        // ✅ Ensure MRN is always set
        patient.setMedicalRecordNumber(
                dto.getMedicalRecordNumber() != null && !dto.getMedicalRecordNumber().isBlank()
                        ? dto.getMedicalRecordNumber()
                        : generateMrn()
        );
        return patient;
    }

    private PatientDto mapToDto(Patient patient) {
        PatientDto dto = new PatientDto();
        dto.setId(patient.getId());
        dto.setExternalId(patient.getExternalId());
        dto.setFirstName(patient.getFirstName());
        dto.setLastName(patient.getLastName());
        dto.setMiddleName(patient.getMiddleName());
        dto.setGender(patient.getGender());
        dto.setDateOfBirth(patient.getDateOfBirth());
        dto.setPhoneNumber(patient.getPhoneNumber());
        dto.setEmail(patient.getEmail());
        dto.setAddress(patient.getAddress());
        dto.setStatus(patient.getStatus());
        dto.setMedicalRecordNumber(patient.getMedicalRecordNumber());
    dto.setTenantName(tenantNameFromOrgId(patient.getOrgId()));
        if (patient.getCreatedDate() != null || patient.getLastModifiedDate() != null) {
            PatientDto.Audit audit = new PatientDto.Audit();
            audit.setCreatedDate(patient.getCreatedDate());
            audit.setLastModifiedDate(patient.getLastModifiedDate());
            dto.setAudit(audit);
        }
        return dto;
    }

    private void updateEntityFromDto(Patient patient, PatientDto dto) {
        if (dto.getFirstName() != null) patient.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) patient.setLastName(dto.getLastName());
        if (dto.getMiddleName() != null) patient.setMiddleName(dto.getMiddleName());
        if (dto.getGender() != null) patient.setGender(dto.getGender());
        if (dto.getDateOfBirth() != null) patient.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getPhoneNumber() != null) patient.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getEmail() != null) patient.setEmail(dto.getEmail());
        if (dto.getAddress() != null) patient.setAddress(dto.getAddress());
        if (dto.getStatus() != null) patient.setStatus(dto.getStatus());

        // ❌ Do NOT allow MRN updates once created
    }

    private Long getCurrentOrgId() {
        String tenant = currentTenantName();
        if (tenant == null || tenant.isBlank()) {
            return null;
        }
        String digits = tenant.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String currentTenantName() {
        RequestContext ctx = RequestContext.get();
        return ctx != null ? ctx.getTenantName() : null;
    }

    private String tenantNameFromOrgId(Long orgId) {
        return orgId == null ? null : "practice_" + orgId;
    }

    private String generateMrn() {
        return "PAT" + System.currentTimeMillis();
    }
}
