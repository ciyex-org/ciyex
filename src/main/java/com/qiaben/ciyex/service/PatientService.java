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
        // Single-tenant: count all patients in the instance
        log.info("Counting all patients for single-tenant instance");
        return repository.count();
    }

    // Create a new patient
    @Transactional
    public PatientDto create(PatientDto dto) {
        

        if (dto.getFirstName() == null || dto.getLastName() == null) {
            throw new IllegalArgumentException("First name and last name are required");
        }

        // ✅ Auto-generate MRN if missing
        if (dto.getMedicalRecordNumber() == null || dto.getMedicalRecordNumber().isBlank()) {
            dto.setMedicalRecordNumber(generateMrn());
        }

        Patient patient = mapToEntity(dto);

        String externalId = null;
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<PatientDto> externalStorage = storageResolver.resolve(PatientDto.class);
                externalId = externalStorage.create(dto);
                log.info("Successfully created patient in external storage with externalId: {}", externalId);
            } catch (Exception e) {
                log.error("Failed to create patient in external storage, error: {}", e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        patient.setExternalId(externalId);

        // Save within tenant schema
        patient = repository.save(patient);
        
        if (patient.getId() == null) {
            log.error("Database save failed to generate id for patient with externalId: {}", externalId);
            throw new RuntimeException("Failed to generate id for new patient");
        }

    dto.setId(patient.getId());
    dto.setExternalId(externalId);
        log.info("Created patient with id: {} and externalId: {} in DB", patient.getId(), externalId);

        return dto;
    }

    // Fetch patient by ID
    @Transactional(readOnly = true)
    public PatientDto getById(Long id) {
        
        Patient patient = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + id));

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
        
        Patient patient = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + id));

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && patient.getExternalId() != null) {
            try {
                ExternalStorage<PatientDto> externalStorage = storageResolver.resolve(PatientDto.class);
                externalStorage.update(dto, patient.getExternalId());
                log.info("Successfully updated patient with id: {} and externalId: {} in external storage", id, patient.getExternalId());
            } catch (Exception e) {
                log.error("Failed to update patient in external storage, error: {}", e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        updateEntityFromDto(patient, dto);
        patient = repository.save(patient);

    dto.setId(patient.getId());
    dto.setExternalId(patient.getExternalId());
        log.info("Updated patient with id: {} and externalId: {} in DB", id, patient.getExternalId());

        return dto;
    }

    // Delete a patient
    @Transactional
    public void delete(Long id) {
        
        Patient patient = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + id));

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && patient.getExternalId() != null) {
            try {
                ExternalStorage<PatientDto> externalStorage = storageResolver.resolve(PatientDto.class);
                externalStorage.delete(patient.getExternalId());
            } catch (Exception e) {
                log.error("Failed to delete patient from external storage, error: {}", e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        repository.delete(patient);
        log.info("Deleted patient with id: {} from DB", id);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<PatientDto>> getAllPatients() {
        
        List<Patient> patients = repository.findAll();
    List<PatientDto> patientDtos = patients.stream().map(this::mapToDto).collect(Collectors.toList());

        return ApiResponse.<List<PatientDto>>builder()
                .success(true)
                .message("Patients retrieved successfully")
                .data(patientDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<PatientDto> searchPatients(String query, Pageable pageable) {
        
        if (query == null || query.isBlank()) {
            log.info("Empty search query provided, returning all patients");
            return repository.findAll(pageable).map(this::mapToDto);
        }

        log.info("Searching patients with query: {}", query);
        return repository.searchBy(query.toLowerCase(), pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<PatientDto> getAllPatients(Pageable pageable, String search) {
        
        Page<Patient> page;
        if (search != null && !search.isBlank()) {
            page = repository.searchBy(search.toLowerCase(), pageable);
        } else {
            page = repository.findAll(pageable);
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
        if (patient.getCreatedDate() != null || patient.getLastModifiedDate() != null) {
            PatientDto.Audit audit = new PatientDto.Audit();
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

    private String generateMrn() {
        return "PAT" + System.currentTimeMillis();
    }
}
