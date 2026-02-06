package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ProviderDto;
import com.qiaben.ciyex.dto.ProviderStatus;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Provider Service - FHIR Only.
 * All provider data is stored in HAPI FHIR server as Practitioner resources.
 */
@Service
@Slf4j
public class ProviderService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String IDENTIFIER_SYSTEM_NPI = "http://hl7.org/fhir/sid/us-npi";
    private static final String IDENTIFIER_SYSTEM_LICENSE = "urn:ciyex:provider:license";
    private static final String EXT_CREATED_DATE = "http://ciyex.com/fhir/StructureDefinition/created-date";
    private static final String EXT_CREATED_BY = "http://ciyex.com/fhir/StructureDefinition/created-by";
    private static final String EXT_MODIFIED_DATE = "http://ciyex.com/fhir/StructureDefinition/last-modified-date";
    private static final String EXT_MODIFIED_BY = "http://ciyex.com/fhir/StructureDefinition/last-modified-by";

    @Autowired
    public ProviderService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // ✅ Create provider in FHIR
    public ProviderDto create(ProviderDto dto) {
        validateMandatoryFields(dto);

        log.info("Creating provider in FHIR: {} {}",
                dto.getIdentification() != null ? dto.getIdentification().getFirstName() : "",
                dto.getIdentification() != null ? dto.getIdentification().getLastName() : "");

        Practitioner fhirPractitioner = toFhirPractitioner(dto);
        addAuditMetadata(fhirPractitioner, true);

        MethodOutcome outcome = fhirClientService.create(fhirPractitioner, getPracticeId());

        String fhirId = outcome.getId().getIdPart();
        dto.setId(parseFhirIdToLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setAudit(extractAudit(fhirPractitioner));

        log.info("Created FHIR Practitioner with ID: {}", fhirId);
        return dto;
    }

    // ✅ Get provider by FHIR ID
    public ProviderDto getById(Long id) {
        return getByFhirId(String.valueOf(id));
    }

    public ProviderDto getByFhirId(String fhirId) {
        log.debug("Reading FHIR Practitioner with ID: {}", fhirId);
        try {
            Practitioner fhirPractitioner = fhirClientService.read(Practitioner.class, fhirId, getPracticeId());
            return toProviderDto(fhirPractitioner);
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("Provider not found with FHIR ID: " + fhirId);
        }
    }

    // ✅ Update provider in FHIR
    public ProviderDto update(Long id, ProviderDto dto) {
        return updateByFhirId(String.valueOf(id), dto);
    }

    public ProviderDto updateByFhirId(String fhirId, ProviderDto dto) {
        log.info("Updating FHIR Practitioner with ID: {}", fhirId);

        Practitioner existing = fhirClientService.read(Practitioner.class, fhirId, getPracticeId());
        Practitioner fhirPractitioner = toFhirPractitioner(dto);
        fhirPractitioner.setId(fhirId);

        preserveCreatedAudit(existing, fhirPractitioner);
        addAuditMetadata(fhirPractitioner, false);

        fhirClientService.update(fhirPractitioner, getPracticeId());

        dto.setId(parseFhirIdToLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setAudit(extractAudit(fhirPractitioner));

        log.info("Updated FHIR Practitioner with ID: {}", fhirId);
        return dto;
    }

    // ✅ Delete provider from FHIR
    public void delete(Long id) {
        deleteByFhirId(String.valueOf(id));
    }

    public void deleteByFhirId(String fhirId) {
        log.info("Deleting FHIR Practitioner with ID: {}", fhirId);
        fhirClientService.delete(Practitioner.class, fhirId, getPracticeId());
        log.info("Deleted FHIR Practitioner with ID: {}", fhirId);
    }

    // ✅ Get all providers from FHIR
    public ApiResponse<List<ProviderDto>> getAllProviders() {
        log.debug("Getting all FHIR Practitioners for practice {}", getPracticeId());

        Bundle bundle = fhirClientService.search(Practitioner.class, getPracticeId());
        List<ProviderDto> dtos = extractPractitioners(bundle);

        return ApiResponse.<List<ProviderDto>>builder()
                .success(true)
                .message("Providers retrieved successfully from FHIR")
                .data(dtos)
                .build();
    }

    // ✅ Get provider count
    public long getProviderCount() {
        Bundle bundle = fhirClientService.search(Practitioner.class, getPracticeId());
        return bundle.getTotal();
    }

    // ✅ Update status (active/inactive in FHIR)
    public ProviderDto updateStatus(Long id, ProviderStatus status) {
        String fhirId = String.valueOf(id);
        log.info("Updating status for FHIR Practitioner with ID: {} to {}", fhirId, status);

        try {
            Practitioner fhirPractitioner = fhirClientService.read(Practitioner.class, fhirId, getPracticeId());
            fhirPractitioner.setActive(status == ProviderStatus.ACTIVE);
            fhirClientService.update(fhirPractitioner, getPracticeId());
            return toProviderDto(fhirPractitioner);
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("Provider not found with FHIR ID: " + fhirId);
        }
    }

    public boolean resetProviderPassword(Long providerId, String newPassword) {
        log.info("Password reset requested for provider id: {}", providerId);
        return true;
    }

    // ========== FHIR Mapping Methods ==========

    private Practitioner toFhirPractitioner(ProviderDto dto) {
        Practitioner practitioner = new Practitioner();

        // NPI Identifier
        if (dto.getNpi() != null) {
            practitioner.addIdentifier()
                    .setSystem(IDENTIFIER_SYSTEM_NPI)
                    .setValue(dto.getNpi())
                    .setUse(Identifier.IdentifierUse.OFFICIAL);
        }

        // License Identifier
        if (dto.getProfessionalDetails() != null && dto.getProfessionalDetails().getLicenseNumber() != null) {
            practitioner.addIdentifier()
                    .setSystem(IDENTIFIER_SYSTEM_LICENSE)
                    .setValue(dto.getProfessionalDetails().getLicenseNumber());
        }

        // Name
        if (dto.getIdentification() != null) {
            HumanName name = practitioner.addName().setUse(HumanName.NameUse.OFFICIAL);
            if (dto.getIdentification().getLastName() != null) {
                name.setFamily(dto.getIdentification().getLastName());
            }
            if (dto.getIdentification().getFirstName() != null) {
                name.addGiven(dto.getIdentification().getFirstName());
            }
            if (dto.getIdentification().getMiddleName() != null) {
                name.addGiven(dto.getIdentification().getMiddleName());
            }
            if (dto.getIdentification().getPrefix() != null) {
                name.addPrefix(dto.getIdentification().getPrefix());
            }
            if (dto.getIdentification().getSuffix() != null) {
                name.addSuffix(dto.getIdentification().getSuffix());
            }

            // Gender
            if (dto.getIdentification().getGender() != null) {
                practitioner.setGender(mapGender(dto.getIdentification().getGender()));
            }

            // Birth Date
            if (dto.getIdentification().getDateOfBirth() != null) {
                practitioner.setBirthDate(parseDate(dto.getIdentification().getDateOfBirth()));
            }
        }

        // Contact Info
        if (dto.getContact() != null) {
            if (dto.getContact().getPhoneNumber() != null) {
                practitioner.addTelecom()
                        .setSystem(ContactPoint.ContactPointSystem.PHONE)
                        .setValue(dto.getContact().getPhoneNumber())
                        .setUse(ContactPoint.ContactPointUse.WORK);
            }
            if (dto.getContact().getMobileNumber() != null) {
                practitioner.addTelecom()
                        .setSystem(ContactPoint.ContactPointSystem.PHONE)
                        .setValue(dto.getContact().getMobileNumber())
                        .setUse(ContactPoint.ContactPointUse.MOBILE);
            }
            if (dto.getContact().getFaxNumber() != null) {
                practitioner.addTelecom()
                        .setSystem(ContactPoint.ContactPointSystem.FAX)
                        .setValue(dto.getContact().getFaxNumber())
                        .setUse(ContactPoint.ContactPointUse.WORK);
            }
            if (dto.getContact().getEmail() != null) {
                practitioner.addTelecom()
                        .setSystem(ContactPoint.ContactPointSystem.EMAIL)
                        .setValue(dto.getContact().getEmail())
                        .setUse(ContactPoint.ContactPointUse.WORK);
            }

            // Address
            if (dto.getContact().getAddress() != null) {
                ProviderDto.Contact.Address addr = dto.getContact().getAddress();
                Address fhirAddr = practitioner.addAddress().setUse(Address.AddressUse.WORK);
                if (addr.getStreet() != null) fhirAddr.addLine(addr.getStreet());
                if (addr.getCity() != null) fhirAddr.setCity(addr.getCity());
                if (addr.getState() != null) fhirAddr.setState(addr.getState());
                if (addr.getPostalCode() != null) fhirAddr.setPostalCode(addr.getPostalCode());
                if (addr.getCountry() != null) fhirAddr.setCountry(addr.getCountry());
            }
        }

        // Qualification (specialty, license)
        if (dto.getProfessionalDetails() != null) {
            Practitioner.PractitionerQualificationComponent qual = practitioner.addQualification();
            CodeableConcept code = qual.getCode();
            if (dto.getProfessionalDetails().getSpecialty() != null) {
                code.setText(dto.getProfessionalDetails().getSpecialty());
            }
            if (dto.getProfessionalDetails().getLicenseState() != null) {
                qual.setIssuer(new Reference().setDisplay(dto.getProfessionalDetails().getLicenseState()));
            }
        }

        // Status
        if (dto.getSystemAccess() != null && dto.getSystemAccess().getStatus() != null) {
            practitioner.setActive(dto.getSystemAccess().getStatus() == ProviderStatus.ACTIVE);
        } else {
            practitioner.setActive(true);
        }

        return practitioner;
    }

    private ProviderDto toProviderDto(Practitioner practitioner) {
        ProviderDto dto = new ProviderDto();

        // FHIR ID and ID
        if (practitioner.hasId()) {
            String fhirId = practitioner.getIdElement().getIdPart();
            dto.setId(parseFhirIdToLong(fhirId));
            dto.setFhirId(fhirId);
        }

        // Identifiers
        for (Identifier identifier : practitioner.getIdentifier()) {
            if (IDENTIFIER_SYSTEM_NPI.equals(identifier.getSystem())) {
                dto.setNpi(identifier.getValue());
            }
        }

        // Name
        ProviderDto.Identification identification = new ProviderDto.Identification();
        if (practitioner.hasName()) {
            HumanName name = practitioner.getNameFirstRep();
            identification.setLastName(name.getFamily());
            if (!name.getGiven().isEmpty()) {
                identification.setFirstName(name.getGiven().get(0).getValue());
            }
            if (name.getGiven().size() > 1) {
                identification.setMiddleName(name.getGiven().get(1).getValue());
            }
            if (!name.getPrefix().isEmpty()) {
                identification.setPrefix(name.getPrefix().get(0).getValue());
            }
            if (!name.getSuffix().isEmpty()) {
                identification.setSuffix(name.getSuffix().get(0).getValue());
            }
        }

        // Gender
        if (practitioner.hasGender()) {
            identification.setGender(mapGenderToString(practitioner.getGender()));
        }

        // Birth Date
        if (practitioner.hasBirthDate()) {
            identification.setDateOfBirth(formatDate(practitioner.getBirthDate()));
        }
        dto.setIdentification(identification);

        // Contact Info
        ProviderDto.Contact contact = new ProviderDto.Contact();
        for (ContactPoint telecom : practitioner.getTelecom()) {
            switch (telecom.getSystem()) {
                case PHONE -> {
                    if (telecom.getUse() == ContactPoint.ContactPointUse.MOBILE) {
                        contact.setMobileNumber(telecom.getValue());
                    } else {
                        contact.setPhoneNumber(telecom.getValue());
                    }
                }
                case FAX -> contact.setFaxNumber(telecom.getValue());
                case EMAIL -> contact.setEmail(telecom.getValue());
                default -> {}
            }
        }

        // Address
        if (practitioner.hasAddress()) {
            Address fhirAddr = practitioner.getAddressFirstRep();
            ProviderDto.Contact.Address addr = new ProviderDto.Contact.Address();
            if (fhirAddr.hasLine() && !fhirAddr.getLine().isEmpty()) {
                addr.setStreet(fhirAddr.getLine().get(0).getValue());
            }
            addr.setCity(fhirAddr.getCity());
            addr.setState(fhirAddr.getState());
            addr.setPostalCode(fhirAddr.getPostalCode());
            addr.setCountry(fhirAddr.getCountry());
            contact.setAddress(addr);
        }
        dto.setContact(contact);

        // Professional Details
        ProviderDto.ProfessionalDetails profDetails = new ProviderDto.ProfessionalDetails();
        if (practitioner.hasQualification()) {
            Practitioner.PractitionerQualificationComponent qual = practitioner.getQualificationFirstRep();
            if (qual.hasCode() && qual.getCode().hasText()) {
                profDetails.setSpecialty(qual.getCode().getText());
            }
            if (qual.hasIssuer() && qual.getIssuer().hasDisplay()) {
                profDetails.setLicenseState(qual.getIssuer().getDisplay());
            }
        }
        for (Identifier identifier : practitioner.getIdentifier()) {
            if (IDENTIFIER_SYSTEM_LICENSE.equals(identifier.getSystem())) {
                profDetails.setLicenseNumber(identifier.getValue());
            }
        }
        dto.setProfessionalDetails(profDetails);

        // System Access
        ProviderDto.SystemAccess systemAccess = new ProviderDto.SystemAccess();
        systemAccess.setStatus(practitioner.getActive() ? ProviderStatus.ACTIVE : ProviderStatus.ARCHIVED);
        dto.setSystemAccess(systemAccess);

        // Extract audit information
        dto.setAudit(extractAudit(practitioner));

        return dto;
    }

    private List<ProviderDto> extractPractitioners(Bundle bundle) {
        List<ProviderDto> providers = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Practitioner) {
                    providers.add(toProviderDto((Practitioner) entry.getResource()));
                }
            }
        }
        return providers;
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
            return null;
        }
    }

    private String formatDate(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault())
                .toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private void validateMandatoryFields(ProviderDto dto) {
        StringBuilder errors = new StringBuilder();

        if (dto.getNpi() == null || dto.getNpi().trim().isEmpty()) {
            errors.append("npi, ");
        }

        if (dto.getIdentification() == null) {
            errors.append("firstName, lastName, ");
        } else {
            if (dto.getIdentification().getFirstName() == null || dto.getIdentification().getFirstName().trim().isEmpty()) {
                errors.append("firstName, ");
            }
            if (dto.getIdentification().getLastName() == null || dto.getIdentification().getLastName().trim().isEmpty()) {
                errors.append("lastName, ");
            }
        }

        if (dto.getContact() == null || dto.getContact().getMobileNumber() == null || dto.getContact().getMobileNumber().trim().isEmpty()) {
            errors.append("mobileNumber, ");
        }

        if (dto.getSystemAccess() == null || dto.getSystemAccess().getStatus() == null) {
            errors.append("status, ");
        }

        if (dto.getProfessionalDetails() == null) {
            errors.append("specialty, providertype, licenseNumber, ");
        } else {
            if (dto.getProfessionalDetails().getSpecialty() == null || dto.getProfessionalDetails().getSpecialty().trim().isEmpty()) {
                errors.append("specialty, ");
            }
            if (dto.getProfessionalDetails().getProviderType() == null || dto.getProfessionalDetails().getProviderType().trim().isEmpty()) {
                errors.append("providertype, ");
            }
            if (dto.getProfessionalDetails().getLicenseNumber() == null || dto.getProfessionalDetails().getLicenseNumber().trim().isEmpty()) {
                errors.append("licenseNumber, ");
            }
        }

        if (errors.length() > 0) {
            String missingFields = errors.substring(0, errors.length() - 2);
            throw new IllegalArgumentException("Missing mandatory fields: " + missingFields);
        }
    }

    // ========== Audit Helper Methods ========== //

    private void addAuditMetadata(Practitioner practitioner, boolean isCreate) {
        String currentUser = getCurrentUser();
        Date now = new Date();

        if (isCreate) {
            practitioner.addExtension(new Extension(EXT_CREATED_DATE, new DateTimeType(now)));
            practitioner.addExtension(new Extension(EXT_CREATED_BY, new StringType(currentUser)));
        }

        practitioner.addExtension(new Extension(EXT_MODIFIED_DATE, new DateTimeType(now)));
        practitioner.addExtension(new Extension(EXT_MODIFIED_BY, new StringType(currentUser)));
    }

    private void preserveCreatedAudit(Practitioner existing, Practitioner updated) {
        Extension createdDate = existing.getExtensionByUrl(EXT_CREATED_DATE);
        Extension createdBy = existing.getExtensionByUrl(EXT_CREATED_BY);

        if (createdDate != null) {
            updated.addExtension(createdDate);
        }
        if (createdBy != null) {
            updated.addExtension(createdBy);
        }
    }

    private ProviderDto.Audit extractAudit(Practitioner practitioner) {
        ProviderDto.Audit audit = new ProviderDto.Audit();

        Extension createdDate = practitioner.getExtensionByUrl(EXT_CREATED_DATE);
        if (createdDate != null && createdDate.getValue() instanceof DateTimeType) {
            audit.setCreatedDate(((DateTimeType) createdDate.getValue()).getValueAsString());
        }

        Extension modifiedDate = practitioner.getExtensionByUrl(EXT_MODIFIED_DATE);
        if (modifiedDate != null && modifiedDate.getValue() instanceof DateTimeType) {
            audit.setLastModifiedDate(((DateTimeType) modifiedDate.getValue()).getValueAsString());
        }

        return audit;
    }

    private String getCurrentUser() {
        try {
            RequestContext context = RequestContext.get();
            if (context != null && context.getAuthToken() != null) {
                return "user-from-token";
            }
        } catch (Exception e) {
            log.debug("Could not extract user from RequestContext: {}", e.getMessage());
        }
        return "system";
    }

    private Long parseFhirIdToLong(String fhirId) {
        if (fhirId == null) return null;
        try {
            return Long.parseLong(fhirId);
        } catch (NumberFormatException e) {
            log.warn("FHIR ID '{}' is not numeric, using hashCode", fhirId);
            return (long) Math.abs(fhirId.hashCode());
        }
    }
}