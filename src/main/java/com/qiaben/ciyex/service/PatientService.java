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

import java.time.format.DateTimeFormatter;
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

        String externalId = dto.getExternalId(); // Start with DTO's externalId
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        log.info("Storage type configured: {} (single-tenant mode, no tenant/org validation required)", storageType);
        if (storageType != null) {
            try {
                ExternalStorage<PatientDto> externalStorage = storageResolver.resolve(PatientDto.class);

                externalId = externalStorage.create(dto); // Override with external storage ID if available
                log.info("Successfully created patient in external storage with externalId: {}", externalId);

                externalId = externalStorage.create(dto);
                log.info("Successfully created patient in external storage with externalId: {} (no tenant context required)", externalId);
            } catch (IllegalStateException e) {
                log.warn("External storage configuration issue, proceeding without external sync: {}", e.getMessage());
                // Continue without external storage - don't fail the entire operation
            } catch (RuntimeException e) {
                if (e.getMessage() != null && (e.getMessage().contains("No FHIR configuration") ||
                        e.getMessage().contains("No tenantName") || e.getMessage().contains("No orgId"))) {
                    log.warn("External storage not configured or tenant context missing, proceeding without external sync: {}", e.getMessage());
                    // Continue without external storage - don't fail the entire operation
                } else {
                    log.error("Unexpected external storage error. Error type: {}, Message: {}",
                            e.getClass().getSimpleName(), e.getMessage(), e);
                    log.warn("Proceeding without external sync due to unexpected error");
                    // Continue without external storage rather than failing
                }

            } catch (Exception e) {
                log.error("External storage sync failed but patient creation will continue. Error type: {}, Message: {}",
                        e.getClass().getSimpleName(), e.getMessage(), e);
                log.warn("Proceeding without external sync due to general error");
                // Continue without external storage rather than failing
            }
        } else {
            log.info("No external storage configured, saving patient to local database only");
        }

        // Auto-generate externalId if not provided
        if (externalId == null) {
            externalId = "PAT-" + System.currentTimeMillis();
            log.info("Auto-generated externalId: {}", externalId);
        }

        patient.setFhirId(externalId);
        patient.setExternalId(externalId);
        patient = repository.save(patient);

        if (patient.getId() == null) {
            log.error("Database save failed to generate id for patient with externalId: {}", externalId);
            throw new RuntimeException("Failed to generate id for new patient");
        }

        log.info("Created patient with id: {} and externalId: {}", patient.getId(), externalId);


        // Map the saved entity to DTO to include audit fields

        return mapToDto(patient);
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

    // ✅ Get patients with search and status filter
    @Transactional(readOnly = true)
    public Page<PatientDto> getPatients(String search, String status, Pageable pageable) {
        Page<Patient> page;
        if (search != null && !search.isBlank()) {
            page = repository.searchBy(search.toLowerCase(), pageable);
        } else {
            page = repository.findAll(pageable);
        }
        
        if (!"all".equalsIgnoreCase(status)) {
            return page.map(this::mapToDto)
                    .filter(dto -> status.equalsIgnoreCase(dto.getStatus()));
        }
        
        return page.map(this::mapToDto);
    }

    // --- Mapping helpers ---
    private Patient mapToEntity(PatientDto dto) {
        Patient patient = new Patient();

        // Use externalId if provided, otherwise use fhirId
        String fhirIdValue = dto.getExternalId() != null ? dto.getExternalId() : dto.getFhirId();
        patient.setFhirId(fhirIdValue);
        patient.setExternalId(fhirIdValue);

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
        dto.setFhirId(patient.getFhirId());
        dto.setExternalId(patient.getFhirId()); // externalId is an alias for fhirId
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
        
        // Map audit fields
        if (patient.getCreatedDate() != null || patient.getLastModifiedDate() != null) {
            PatientDto.Audit audit = new PatientDto.Audit();
            if (patient.getCreatedDate() != null) {
                audit.setCreatedDate(patient.getCreatedDate().toString());
            }
            if (patient.getLastModifiedDate() != null) {
                audit.setLastModifiedDate(patient.getLastModifiedDate().toString());
            }
            dto.setAudit(audit);
        }
        


        // Set audit information
        PatientDto.Audit audit = new PatientDto.Audit();
        if (patient.getCreatedDate() != null) {
            audit.setCreatedDate(patient.getCreatedDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (patient.getLastModifiedDate() != null) {
            audit.setLastModifiedDate(patient.getLastModifiedDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        dto.setAudit(audit);


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