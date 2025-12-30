package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PatientDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Patient Service - FHIR Only.
 * All patient data is stored in HAPI FHIR server.
 */
@Service
@Slf4j
public class PatientService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String IDENTIFIER_SYSTEM_MRN = "urn:ciyex:patient:mrn";
    private static final String IDENTIFIER_SYSTEM_EXTERNAL = "urn:ciyex:patient:external-id";

    @Autowired
    public PatientService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
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

    // ✅ Count all patients from FHIR
    public long countPatientsForCurrentOrg() {
        log.info("Counting all patients from FHIR");
        try {
            Bundle bundle = fhirClientService.getClient().search()
                    .forResource(Patient.class)
                    .withAdditionalHeader("X-Request-Tenant-Id", getPracticeId())
                    .returnBundle(Bundle.class)
                    .execute();
            return bundle.getTotal();
        } catch (Exception e) {
            log.error("Failed to count patients from FHIR: {}", e.getMessage());
            return 0;
        }
    }

    // ✅ Create a new patient in FHIR
    public PatientDto create(PatientDto dto) {
        validatePatientFields(dto);

        // Auto-generate MRN if missing
        if (dto.getMedicalRecordNumber() == null || dto.getMedicalRecordNumber().isBlank()) {
            dto.setMedicalRecordNumber(generateMrn());
        }

        log.info("Creating patient in FHIR for practice {}", getPracticeId());
        
        Patient fhirPatient = toFhirPatient(dto);
        fhirPatient.setManagingOrganization(new Reference("Organization/" + getPracticeId()));
        
        MethodOutcome outcome = fhirClientService.create(fhirPatient, getPracticeId());
        
        String fhirId = outcome.getId().getIdPart();
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        
        log.info("Created FHIR patient with ID: {}", fhirId);
        return dto;
    }

    // ✅ Retrieve patient by FHIR ID
    public PatientDto getById(Long id) {
        // For backward compatibility, treat Long id as string FHIR ID
        return getByFhirId(String.valueOf(id));
    }

    // ✅ Retrieve patient by FHIR ID (string)
    public PatientDto getByFhirId(String fhirId) {
        log.debug("Reading FHIR patient with ID: {}", fhirId);
        
        try {
            Patient fhirPatient = fhirClientService.read(Patient.class, fhirId, getPracticeId());
            return toPatientDto(fhirPatient);
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("Patient not found with FHIR ID: " + fhirId);
        }
    }

    // ✅ Update patient in FHIR
    public PatientDto update(Long id, PatientDto dto) {
        return updateByFhirId(String.valueOf(id), dto);
    }

    // ✅ Update patient by FHIR ID
    public PatientDto updateByFhirId(String fhirId, PatientDto dto) {
        validatePatientFields(dto);
        
        log.info("Updating FHIR patient with ID: {}", fhirId);
        
        Patient fhirPatient = toFhirPatient(dto);
        fhirPatient.setId(fhirId);
        
        fhirClientService.update(fhirPatient, getPracticeId());
        
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        
        log.info("Updated FHIR patient with ID: {}", fhirId);
        return dto;
    }

    // ✅ Delete patient from FHIR
    public void delete(Long id) {
        deleteByFhirId(String.valueOf(id));
    }

    // ✅ Delete patient by FHIR ID
    public void deleteByFhirId(String fhirId) {
        log.info("Deleting FHIR patient with ID: {}", fhirId);
        fhirClientService.delete(Patient.class, fhirId, getPracticeId());
        log.info("Deleted FHIR patient with ID: {}", fhirId);
    }

    // ✅ Get all patients from FHIR
    public ApiResponse<List<PatientDto>> getAllPatients() {
        log.debug("Getting all FHIR patients for practice {}", getPracticeId());
        
        Bundle bundle = fhirClientService.search(Patient.class, getPracticeId());
        List<PatientDto> dtos = extractPatients(bundle);

        return ApiResponse.<List<PatientDto>>builder()
                .success(true)
                .message("Patients retrieved successfully from FHIR")
                .data(dtos)
                .build();
    }

    // ✅ Search patients in FHIR
    public Page<PatientDto> searchPatients(String query, Pageable pageable) {
        return getAllPatients(pageable, query);
    }

    // ✅ Get all patients with pagination and search
    public Page<PatientDto> getAllPatients(Pageable pageable, String search) {
        log.debug("Searching FHIR patients with query: {}", search);
        
        List<PatientDto> allPatients;
        
        if (search != null && !search.isBlank()) {
            Bundle bundle = fhirClientService.getClient().search()
                    .forResource(Patient.class)
                    .where(new StringClientParam("name").matches().value(search))
                    .withAdditionalHeader("X-Request-Tenant-Id", getPracticeId())
                    .returnBundle(Bundle.class)
                    .execute();
            allPatients = extractPatients(bundle);
        } else {
            Bundle bundle = fhirClientService.search(Patient.class, getPracticeId());
            allPatients = extractPatients(bundle);
        }
        
        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allPatients.size());
        
        List<PatientDto> pageContent = start < allPatients.size() 
                ? allPatients.subList(start, end) 
                : new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageable, allPatients.size());
    }

    // ✅ Get patients with search and status filter
    public Page<PatientDto> getPatients(String search, String status, Pageable pageable) {
        Page<PatientDto> page = getAllPatients(pageable, search);
        
        if (!"all".equalsIgnoreCase(status) && status != null) {
            List<PatientDto> filtered = page.getContent().stream()
                    .filter(dto -> status.equalsIgnoreCase(dto.getStatus()))
                    .collect(Collectors.toList());
            return new PageImpl<>(filtered, pageable, filtered.size());
        }
        
        return page;
    }

    // ========== FHIR Mapping Methods ==========

    private Patient toFhirPatient(PatientDto dto) {
        Patient patient = new Patient();

        // Identifiers
        if (dto.getMedicalRecordNumber() != null) {
            patient.addIdentifier()
                    .setSystem(IDENTIFIER_SYSTEM_MRN)
                    .setValue(dto.getMedicalRecordNumber())
                    .setUse(Identifier.IdentifierUse.OFFICIAL);
        }

        if (dto.getExternalId() != null) {
            patient.addIdentifier()
                    .setSystem(IDENTIFIER_SYSTEM_EXTERNAL)
                    .setValue(dto.getExternalId())
                    .setUse(Identifier.IdentifierUse.SECONDARY);
        }

        // Name
        HumanName name = patient.addName()
                .setUse(HumanName.NameUse.OFFICIAL)
                .setFamily(dto.getLastName());
        
        if (dto.getFirstName() != null) {
            name.addGiven(dto.getFirstName());
        }
        if (dto.getMiddleName() != null) {
            name.addGiven(dto.getMiddleName());
        }

        // Gender
        if (dto.getGender() != null) {
            patient.setGender(mapGender(dto.getGender()));
        }

        // Birth Date
        if (dto.getDateOfBirth() != null) {
            patient.setBirthDate(parseDate(dto.getDateOfBirth()));
        }

        // Contact Info
        if (dto.getPhoneNumber() != null) {
            patient.addTelecom()
                    .setSystem(ContactPoint.ContactPointSystem.PHONE)
                    .setValue(dto.getPhoneNumber())
                    .setUse(ContactPoint.ContactPointUse.MOBILE);
        }

        if (dto.getEmail() != null) {
            patient.addTelecom()
                    .setSystem(ContactPoint.ContactPointSystem.EMAIL)
                    .setValue(dto.getEmail())
                    .setUse(ContactPoint.ContactPointUse.HOME);
        }

        // Address
        if (dto.getAddress() != null) {
            patient.addAddress()
                    .setUse(Address.AddressUse.HOME)
                    .setText(dto.getAddress());
        }

        // Status
        patient.setActive(!"inactive".equalsIgnoreCase(dto.getStatus()));

        return patient;
    }

    private PatientDto toPatientDto(Patient fhirPatient) {
        PatientDto dto = new PatientDto();

        // FHIR ID
        if (fhirPatient.hasId()) {
            dto.setFhirId(fhirPatient.getIdElement().getIdPart());
            dto.setExternalId(fhirPatient.getIdElement().getIdPart());
        }

        // Identifiers
        for (Identifier identifier : fhirPatient.getIdentifier()) {
            if (IDENTIFIER_SYSTEM_MRN.equals(identifier.getSystem())) {
                dto.setMedicalRecordNumber(identifier.getValue());
            }
        }

        // Name
        if (fhirPatient.hasName()) {
            HumanName name = fhirPatient.getNameFirstRep();
            dto.setLastName(name.getFamily());
            
            List<StringType> given = name.getGiven();
            if (!given.isEmpty()) {
                dto.setFirstName(given.get(0).getValue());
            }
            if (given.size() > 1) {
                dto.setMiddleName(given.get(1).getValue());
            }
        }

        // Gender
        if (fhirPatient.hasGender()) {
            dto.setGender(mapGenderToString(fhirPatient.getGender()));
        }

        // Birth Date
        if (fhirPatient.hasBirthDate()) {
            dto.setDateOfBirth(formatDate(fhirPatient.getBirthDate()));
        }

        // Contact Info
        for (ContactPoint telecom : fhirPatient.getTelecom()) {
            if (telecom.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                dto.setPhoneNumber(telecom.getValue());
            } else if (telecom.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                dto.setEmail(telecom.getValue());
            }
        }

        // Address
        if (fhirPatient.hasAddress()) {
            Address address = fhirPatient.getAddressFirstRep();
            if (address.hasText()) {
                dto.setAddress(address.getText());
            } else if (address.hasLine()) {
                dto.setAddress(String.join(", ", 
                        address.getLine().stream()
                                .map(StringType::getValue)
                                .toList()));
            }
        }

        // Status
        dto.setStatus(fhirPatient.getActive() ? "Active" : "Inactive");

        return dto;
    }

    private List<PatientDto> extractPatients(Bundle bundle) {
        List<PatientDto> patients = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Patient) {
                    patients.add(toPatientDto((Patient) entry.getResource()));
                }
            }
        }
        return patients;
    }

    private Enumerations.AdministrativeGender mapGender(String gender) {
        if (gender == null) return Enumerations.AdministrativeGender.UNKNOWN;
        
        return switch (gender.toLowerCase()) {
            case "male", "m" -> Enumerations.AdministrativeGender.MALE;
            case "female", "f" -> Enumerations.AdministrativeGender.FEMALE;
            case "other", "o" -> Enumerations.AdministrativeGender.OTHER;
            default -> Enumerations.AdministrativeGender.UNKNOWN;
        };
    }

    private String mapGenderToString(Enumerations.AdministrativeGender gender) {
        return switch (gender) {
            case MALE -> "Male";
            case FEMALE -> "Female";
            case OTHER -> "Other";
            default -> "Unknown";
        };
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null) return null;
        try {
            LocalDate localDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            try {
                LocalDate localDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private String formatDate(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault())
                .toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String generateMrn() {
        return "MRN-" + System.currentTimeMillis();
    }
}