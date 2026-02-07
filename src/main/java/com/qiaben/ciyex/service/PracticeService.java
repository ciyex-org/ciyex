package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PracticeDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Practice Service - FHIR Only.
 * All practice data is stored in HAPI FHIR server as Organization resources.
 */
@Service
@Slf4j
public class PracticeService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String IDENTIFIER_SYSTEM_PRACTICE = "urn:ciyex:practice:id";
    private static final String IDENTIFIER_SYSTEM_NPI = "http://hl7.org/fhir/sid/us-npi";
    private static final String IDENTIFIER_SYSTEM_TAX = "urn:oid:2.16.840.1.113883.4.4";

    @Autowired
    public PracticeService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // ✅ Delete all existing practices
    public void deleteAllPractices() {
        log.info("Deleting all existing practices");
        try {
            Bundle bundle = fhirClientService.search(Organization.class, getPracticeId());
            List<PracticeDto> practices = extractOrganizations(bundle);
            
            for (PracticeDto practice : practices) {
                if (practice.getFhirId() != null) {
                    try {
                        fhirClientService.delete(Organization.class, practice.getFhirId(), getPracticeId());
                        log.info("Deleted practice with FHIR ID: {}", practice.getFhirId());
                    } catch (Exception e) {
                        log.error("Failed to delete practice with FHIR ID: {}", practice.getFhirId(), e);
                    }
                }
            }
            log.info("Deleted {} practices", practices.size());
        } catch (Exception e) {
            log.error("Error deleting all practices", e);
            throw new RuntimeException("Failed to delete all practices: " + e.getMessage(), e);
        }
    }

    // ✅ Get or create the single practice instance
    public PracticeDto getOrCreatePractice() {
        Bundle bundle = fhirClientService.search(Organization.class, getPracticeId());
        List<PracticeDto> practices = extractOrganizations(bundle);
        
        if (practices.isEmpty()) {
            // Create default practice if none exists
            PracticeDto defaultPractice = new PracticeDto();
            defaultPractice.setName("Default Practice");
            return createSingle(defaultPractice);
        }
        
        // Return the first (and should be only) practice
        return practices.get(0);
    }

    // ✅ Create or update the single practice (upsert)
    public PracticeDto create(PracticeDto dto) {
        Bundle bundle = fhirClientService.search(Organization.class, getPracticeId());
        List<PracticeDto> existing = extractOrganizations(bundle);
        
        if (!existing.isEmpty()) {
            // Update existing practice instead of creating new
            PracticeDto existingPractice = existing.get(0);
            log.info("Practice already exists, updating instead: {}", existingPractice.getFhirId());
            return updateByFhirId(existingPractice.getFhirId(), dto);
        }
        
        return createSingle(dto);
    }

    // ✅ Internal method to create a single practice
    private PracticeDto createSingle(PracticeDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Practice name is required");
        }

        // Set audit fields automatically
        PracticeDto.Audit audit = new PracticeDto.Audit();
        String now = java.time.Instant.now().toString();
        audit.setCreatedDate(now);
        audit.setLastModifiedDate(now);
        audit.setCreatedBy("system");
        audit.setLastModifiedBy("system");
        dto.setAudit(audit);

        log.info("Creating practice in FHIR: {}", dto.getName());

        Organization fhirOrg = toFhirOrganization(dto);

        MethodOutcome outcome = fhirClientService.create(fhirOrg, getPracticeId());

        String fhirId = outcome.getId().getIdPart();
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        log.info("Created FHIR Organization with ID: {}", fhirId);
        return dto;
    }

    // ✅ Get practice by ID (returns existing practice only, no creation)
    public PracticeDto getById(Long id) {
        Bundle bundle = fhirClientService.search(Organization.class, getPracticeId());
        List<PracticeDto> practices = extractOrganizations(bundle);
        return practices.isEmpty() ? null : practices.get(0);
    }

    public PracticeDto getByFhirId(String fhirId) {
        log.debug("Reading FHIR Organization with ID: {}", fhirId);
        try {
            Organization fhirOrg = fhirClientService.read(Organization.class, fhirId, getPracticeId());
            return toPracticeDto(fhirOrg);
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("Practice not found with FHIR ID: " + fhirId);
        }
    }

    // ✅ Update practice in FHIR
    public PracticeDto update(Long id, PracticeDto dto) {
        return updateByFhirId(String.valueOf(id), dto);
    }

    public PracticeDto updateByFhirId(String fhirId, PracticeDto dto) {
        log.info("Updating FHIR Organization with ID: {}", fhirId);

        // Set or update audit fields automatically
        PracticeDto.Audit audit = dto.getAudit();
        if (audit == null) {
            audit = new PracticeDto.Audit();
            audit.setCreatedDate(java.time.Instant.now().toString());
            audit.setCreatedBy("system"); // Replace with user if available
        }
        audit.setLastModifiedDate(java.time.Instant.now().toString());
        audit.setLastModifiedBy("system"); // Replace with user if available
        dto.setAudit(audit);

        Organization fhirOrg = toFhirOrganization(dto);
        fhirOrg.setId(fhirId);

        fhirClientService.update(fhirOrg, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        log.info("Updated FHIR Organization with ID: {}", fhirId);
        return dto;
    }

    // ✅ Delete practice from FHIR
    public void delete(Long id) {
        deleteByFhirId(String.valueOf(id));
    }

    public void deleteByFhirId(String fhirId) {
        log.info("Deleting FHIR Organization with ID: {}", fhirId);
        fhirClientService.delete(Organization.class, fhirId, getPracticeId());
        log.info("Deleted FHIR Organization with ID: {}", fhirId);
    }

    // ✅ Get all practices from FHIR
    public ApiResponse<List<PracticeDto>> getAllPractices() {
        log.debug("Getting all FHIR Organizations for practice {}", getPracticeId());
        
        Bundle bundle = fhirClientService.search(Organization.class, getPracticeId());
        List<PracticeDto> dtos = extractOrganizations(bundle);

        return ApiResponse.<List<PracticeDto>>builder()
                .success(true)
                .message("Practices retrieved successfully from FHIR")
                .data(dtos)
                .build();
    }

    // ✅ Get practice count
    public long getPracticeCount() {
        Bundle bundle = fhirClientService.search(Organization.class, getPracticeId());
        return bundle.getTotal();
    }

    // ✅ Search by name
    public ApiResponse<List<PracticeDto>> getPracticesByName(String name) {
        log.debug("Searching FHIR Organizations by name: {}", name);
        
        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Organization.class)
                .where(new StringClientParam("name").matches().value(name))
                
                .returnBundle(Bundle.class)
                .execute();
        
        List<PracticeDto> dtos = extractOrganizations(bundle);

        return ApiResponse.<List<PracticeDto>>builder()
                .success(true)
                .message("Practices retrieved successfully")
                .data(dtos)
                .build();
    }

    // ========== FHIR Mapping Methods ==========

    private Organization toFhirOrganization(PracticeDto dto) {
        Organization org = new Organization();

        // Name
        org.setName(dto.getName());

        // Type - Healthcare Provider
        org.addType()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/organization-type")
                .setCode("prov")
                .setDisplay("Healthcare Provider");

        // Active
        org.setActive(true);

        // Contact Info
        if (dto.getContact() != null) {
            PracticeDto.Contact contact = dto.getContact();
            
            if (contact.getPhoneNumber() != null) {
                org.addTelecom()
                        .setSystem(ContactPoint.ContactPointSystem.PHONE)
                        .setValue(contact.getPhoneNumber())
                        .setUse(ContactPoint.ContactPointUse.WORK);
            }
            if (contact.getFaxNumber() != null) {
                org.addTelecom()
                        .setSystem(ContactPoint.ContactPointSystem.FAX)
                        .setValue(contact.getFaxNumber())
                        .setUse(ContactPoint.ContactPointUse.WORK);
            }
            if (contact.getEmail() != null) {
                org.addTelecom()
                        .setSystem(ContactPoint.ContactPointSystem.EMAIL)
                        .setValue(contact.getEmail())
                        .setUse(ContactPoint.ContactPointUse.WORK);
            }

            // Address
            if (contact.getAddress() != null) {
                PracticeDto.Contact.Address addr = contact.getAddress();
                Address fhirAddr = org.addAddress().setUse(Address.AddressUse.WORK);
                if (addr.getLine1() != null) fhirAddr.addLine(addr.getLine1());
                if (addr.getLine2() != null) fhirAddr.addLine(addr.getLine2());
                if (addr.getCity() != null) fhirAddr.setCity(addr.getCity());
                if (addr.getState() != null) fhirAddr.setState(addr.getState());
                if (addr.getPostalCode() != null) fhirAddr.setPostalCode(addr.getPostalCode());
                if (addr.getCountry() != null) fhirAddr.setCountry(addr.getCountry());
            }
        }

        // Practice Settings as Extensions
        if (dto.getPracticeSettings() != null) {
            PracticeDto.PracticeSettings settings = dto.getPracticeSettings();
            if (settings.getEnablePatientPractice() != null) {
                org.addExtension("urn:ciyex:practice:enablePatientPractice", new BooleanType(settings.getEnablePatientPractice()));
            }
            if (settings.getSessionTimeoutMinutes() != null) {
                org.addExtension("urn:ciyex:practice:sessionTimeoutMinutes", new IntegerType(settings.getSessionTimeoutMinutes()));
            }
            if (settings.getTokenExpiryMinutes() != null) {
                org.addExtension("urn:ciyex:practice:tokenExpiryMinutes", new IntegerType(settings.getTokenExpiryMinutes()));
            }
        }

        // Regional Settings as Extensions
        if (dto.getRegionalSettings() != null) {
            PracticeDto.RegionalSettings regional = dto.getRegionalSettings();
            if (regional.getUnitsForVisitForms() != null) {
                org.addExtension("urn:ciyex:practice:unitsForVisitForms", new StringType(regional.getUnitsForVisitForms()));
            }
            if (regional.getDisplayFormatUSWeights() != null) {
                org.addExtension("urn:ciyex:practice:displayFormatUSWeights", new StringType(regional.getDisplayFormatUSWeights()));
            }
            if (regional.getTelephoneCountryCode() != null) {
                org.addExtension("urn:ciyex:practice:telephoneCountryCode", new StringType(regional.getTelephoneCountryCode()));
            }
            if (regional.getDateDisplayFormat() != null) {
                org.addExtension("urn:ciyex:practice:dateDisplayFormat", new StringType(regional.getDateDisplayFormat()));
            }
            if (regional.getTimeDisplayFormat() != null) {
                org.addExtension("urn:ciyex:practice:timeDisplayFormat", new StringType(regional.getTimeDisplayFormat()));
            }
            if (regional.getTimeZone() != null) {
                org.addExtension("urn:ciyex:practice:timeZone", new StringType(regional.getTimeZone()));
            }
            if (regional.getCurrencyDesignator() != null) {
                org.addExtension("urn:ciyex:practice:currencyDesignator", new StringType(regional.getCurrencyDesignator()));
            }
        }

        return org;
    }

    private PracticeDto toPracticeDto(Organization org) {
        PracticeDto dto = new PracticeDto();

        // FHIR ID
        if (org.hasId()) {
            dto.setFhirId(org.getIdElement().getIdPart());
            dto.setExternalId(org.getIdElement().getIdPart());
        }

        // Name
        dto.setName(org.getName());

        // Contact Info
        PracticeDto.Contact contact = new PracticeDto.Contact();
        boolean hasContact = false;
        
        for (ContactPoint telecom : org.getTelecom()) {
            switch (telecom.getSystem()) {
                case PHONE -> { contact.setPhoneNumber(telecom.getValue()); hasContact = true; }
                case FAX -> { contact.setFaxNumber(telecom.getValue()); hasContact = true; }
                case EMAIL -> { contact.setEmail(telecom.getValue()); hasContact = true; }
                default -> {}
            }
        }

        // Address
        if (org.hasAddress()) {
            Address fhirAddr = org.getAddressFirstRep();
            PracticeDto.Contact.Address addr = new PracticeDto.Contact.Address();
            if (fhirAddr.hasLine() && !fhirAddr.getLine().isEmpty()) {
                addr.setLine1(fhirAddr.getLine().get(0).getValue());
                if (fhirAddr.getLine().size() > 1) {
                    addr.setLine2(fhirAddr.getLine().get(1).getValue());
                }
            }
            addr.setCity(fhirAddr.getCity());
            addr.setState(fhirAddr.getState());
            addr.setPostalCode(fhirAddr.getPostalCode());
            addr.setCountry(fhirAddr.getCountry());
            contact.setAddress(addr);
            hasContact = true;
        }

        if (hasContact) {
            dto.setContact(contact);
        }

        // Extract Practice Settings from Extensions
        PracticeDto.PracticeSettings settings = new PracticeDto.PracticeSettings();
        boolean hasSettings = false;
        for (Extension ext : org.getExtension()) {
            switch (ext.getUrl()) {
                case "urn:ciyex:practice:enablePatientPractice" -> {
                    if (ext.getValue() instanceof BooleanType) {
                        settings.setEnablePatientPractice(((BooleanType) ext.getValue()).booleanValue());
                        hasSettings = true;
                    }
                }
                case "urn:ciyex:practice:sessionTimeoutMinutes" -> {
                    if (ext.getValue() instanceof IntegerType) {
                        settings.setSessionTimeoutMinutes(((IntegerType) ext.getValue()).getValue());
                        hasSettings = true;
                    }
                }
                case "urn:ciyex:practice:tokenExpiryMinutes" -> {
                    if (ext.getValue() instanceof IntegerType) {
                        settings.setTokenExpiryMinutes(((IntegerType) ext.getValue()).getValue());
                        hasSettings = true;
                    }
                }
            }
        }
        if (hasSettings) {
            dto.setPracticeSettings(settings);
        }

        // Extract Regional Settings from Extensions
        PracticeDto.RegionalSettings regional = new PracticeDto.RegionalSettings();
        boolean hasRegional = false;
        for (Extension ext : org.getExtension()) {
            switch (ext.getUrl()) {
                case "urn:ciyex:practice:unitsForVisitForms" -> {
                    if (ext.getValue() instanceof StringType) {
                        regional.setUnitsForVisitForms(((StringType) ext.getValue()).getValue());
                        hasRegional = true;
                    }
                }
                case "urn:ciyex:practice:displayFormatUSWeights" -> {
                    if (ext.getValue() instanceof StringType) {
                        regional.setDisplayFormatUSWeights(((StringType) ext.getValue()).getValue());
                        hasRegional = true;
                    }
                }
                case "urn:ciyex:practice:telephoneCountryCode" -> {
                    if (ext.getValue() instanceof StringType) {
                        regional.setTelephoneCountryCode(((StringType) ext.getValue()).getValue());
                        hasRegional = true;
                    }
                }
                case "urn:ciyex:practice:dateDisplayFormat" -> {
                    if (ext.getValue() instanceof StringType) {
                        regional.setDateDisplayFormat(((StringType) ext.getValue()).getValue());
                        hasRegional = true;
                    }
                }
                case "urn:ciyex:practice:timeDisplayFormat" -> {
                    if (ext.getValue() instanceof StringType) {
                        regional.setTimeDisplayFormat(((StringType) ext.getValue()).getValue());
                        hasRegional = true;
                    }
                }
                case "urn:ciyex:practice:timeZone" -> {
                    if (ext.getValue() instanceof StringType) {
                        regional.setTimeZone(((StringType) ext.getValue()).getValue());
                        hasRegional = true;
                    }
                }
                case "urn:ciyex:practice:currencyDesignator" -> {
                    if (ext.getValue() instanceof StringType) {
                        regional.setCurrencyDesignator(((StringType) ext.getValue()).getValue());
                        hasRegional = true;
                    }
                }
            }
        }
        if (hasRegional) {
            dto.setRegionalSettings(regional);
        }

        return dto;
    }

    private List<PracticeDto> extractOrganizations(Bundle bundle) {
        List<PracticeDto> practices = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Organization) {
                    practices.add(toPracticeDto((Organization) entry.getResource()));
                }
            }
        }
        return practices;
    }
}