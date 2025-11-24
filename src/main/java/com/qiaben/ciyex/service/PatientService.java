package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PatientDto;
import com.qiaben.ciyex.entity.Patient;
import com.qiaben.ciyex.repository.PatientRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // ✅ Manual validation for mandatory fields
    private void validatePatientFields(PatientDto dto) {
        StringBuilder errors = new StringBuilder();

        if (dto.getFirstName() == null || dto.getFirstName().isBlank())
            errors.append("First name is required. ");
        if (dto.getLastName() == null || dto.getLastName().isBlank())
            errors.append("Last name is required. ");
        if (dto.getGender() == null || dto.getGender().isBlank())
            errors.append("Gender is required. ");
        if (dto.getDateOfBirth() == null || dto.getDateOfBirth().isBlank())
            errors.append("Date of birth is required. ");
        if (dto.getPhoneNumber() == null || dto.getPhoneNumber().isBlank())
            errors.append("Phone number is required. ");

        if (errors.length() > 0)
            throw new IllegalArgumentException(errors.toString().trim());
    }

    // ✅ Count all patients
    @Transactional(readOnly = true)
    public long countPatientsForCurrentOrg() {
        log.info("Counting all patients for single-tenant instance");
        return repository.count();
    }

    // ✅ Create a new patient with manual validation
    @Transactional
    public PatientDto create(PatientDto dto) {
        // Validate all required fields
        validatePatientFields(dto);

        // Auto-generate MRN if missing
        if (dto.getMedicalRecordNumber() == null || dto.getMedicalRecordNumber().isBlank()) {
            dto.setMedicalRecordNumber(generateMrn());
        }

        Patient patient = mapToEntity(dto);

        String externalId = null;
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        log.info("Storage type configured: {}", storageType);
        if (storageType != null) {
            try {
                ExternalStorage<PatientDto> externalStorage = storageResolver.resolve(PatientDto.class);
                externalId = externalStorage.create(dto);
                log.info("Successfully created patient in external storage with externalId: {}", externalId);
            } catch (IllegalStateException e) {
                log.warn("FHIR configuration error, proceeding without external storage sync: {}", e.getMessage());
                // Continue without FHIR - don't fail the entire operation
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().contains("No FHIR configuration")) {
                    log.warn("FHIR not configured, proceeding without external storage sync: {}", e.getMessage());
                    // Continue without FHIR - don't fail the entire operation
                } else {
                    log.error("Failed to create patient in external storage. Error type: {}, Message: {}", 
                        e.getClass().getSimpleName(), e.getMessage(), e);
                    throw new RuntimeException("Failed to sync with external storage: " + e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error("Failed to create patient in external storage. Error type: {}, Message: {}", 
                    e.getClass().getSimpleName(), e.getMessage(), e);
                throw new RuntimeException("Failed to sync with external storage: " + e.getMessage(), e);
            }
        }

        patient.setExternalId(externalId);
        patient = repository.save(patient);

        if (patient.getId() == null) {
            log.error("Database save failed to generate id for patient with externalId: {}", externalId);
            throw new RuntimeException("Failed to generate id for new patient");
        }

        dto.setId(patient.getId());
        dto.setExternalId(externalId);
        log.info("Created patient with id: {} and externalId: {}", patient.getId(), externalId);

        return dto;
    }

    // ✅ Retrieve patient by ID
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

    // ✅ Fetch patient from external storage (FHIR)
    public PatientDto getPatientFromFhir(String externalId) {
        if (externalId == null) return null;
        ExternalStorage<PatientDto> externalStorage = storageResolver.resolve(PatientDto.class);
        return externalStorage.get(externalId);
    }

    // ✅ Update patient with validation
    @Transactional
    public PatientDto update(Long id, PatientDto dto) {
        // Validate before update
        validatePatientFields(dto);

        Patient patient = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + id));

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && patient.getExternalId() != null) {
            try {
                ExternalStorage<PatientDto> externalStorage = storageResolver.resolve(PatientDto.class);
                externalStorage.update(dto, patient.getExternalId());
                log.info("Updated patient with id: {} and externalId: {} in external storage", id, patient.getExternalId());
            } catch (Exception e) {
                log.error("Failed to update patient in external storage: {}", e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        updateEntityFromDto(patient, dto);
        patient = repository.save(patient);

        dto.setId(patient.getId());
        dto.setExternalId(patient.getExternalId());
        log.info("Updated patient with id: {} and externalId: {}", id, patient.getExternalId());

        return dto;
    }

    // ✅ Delete patient
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
                log.error("Failed to delete patient from external storage: {}", e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        repository.delete(patient);
        log.info("Deleted patient with id: {}", id);
    }

    // ✅ Get all patients
    @Transactional(readOnly = true)
    public ApiResponse<List<PatientDto>> getAllPatients() {
        List<Patient> patients = repository.findAll();
        List<PatientDto> dtos = patients.stream().map(this::mapToDto).collect(Collectors.toList());

        return ApiResponse.<List<PatientDto>>builder()
                .success(true)
                .message("Patients retrieved successfully")
                .data(dtos)
                .build();
    }

    // ✅ Search and pagination
    @Transactional(readOnly = true)
    public Page<PatientDto> searchPatients(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return repository.findAll(pageable).map(this::mapToDto);
        }
        return repository.searchBy(query.toLowerCase(), pageable).map(this::mapToDto);
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

    // --- Mapping helpers ---
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
        patient.setMedicalRecordNumber(dto.getMedicalRecordNumber());
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
    }

    private String generateMrn() {
        return "PAT" + System.currentTimeMillis();
    }
}
