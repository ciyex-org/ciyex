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

    // ✅ Create practice in FHIR
    public PracticeDto create(PracticeDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Practice name is required");
        }

        log.info("Creating practice in FHIR: {}", dto.getName());
        
        Organization fhirOrg = toFhirOrganization(dto);
        
        MethodOutcome outcome = fhirClientService.create(fhirOrg, getPracticeId());
        
        String fhirId = outcome.getId().getIdPart();
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        
        log.info("Created FHIR Organization with ID: {}", fhirId);
        return dto;
    }

    // ✅ Get practice by FHIR ID
    public PracticeDto getById(Long id) {
        return getByFhirId(String.valueOf(id));
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