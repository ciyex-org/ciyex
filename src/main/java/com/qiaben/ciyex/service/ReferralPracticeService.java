package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ReferralPracticeDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only ReferralPractice Service.
 * Uses FHIR Organization resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralPracticeService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // Extension URLs
    private static final String EXT_NPI_ID = "http://ciyex.com/fhir/StructureDefinition/npi-id";
    private static final String EXT_TAX_ID = "http://ciyex.com/fhir/StructureDefinition/tax-id";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public ReferralPracticeDto create(ReferralPracticeDto dto) {
        validateMandatoryFields(dto);

        log.debug("Creating FHIR Organization (referral practice): {}", dto.getName());

        Organization org = toFhirOrganization(dto);
        var outcome = fhirClientService.create(org, getPracticeId());
        String fhirId = outcome.getId().getIdPart();
        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);

        // Set audit information
        ReferralPracticeDto.Audit audit = new ReferralPracticeDto.Audit();
        audit.setCreatedDate(java.time.LocalDateTime.now().toString());
        audit.setLastModifiedDate(java.time.LocalDateTime.now().toString());
        dto.setAudit(audit);

        log.info("Created FHIR Organization (referral practice) with id: {}", fhirId);

        return dto;
    }

    // GET BY ID
    public ReferralPracticeDto getById(String fhirId) {
        log.debug("Getting FHIR Organization (referral practice): {}", fhirId);
        Organization org = fhirClientService.read(Organization.class, fhirId, getPracticeId());
        return fromFhirOrganization(org);
    }

    // GET ALL
    public List<ReferralPracticeDto> getAll() {
        log.debug("Getting all FHIR Organizations (referral practice)");

        Bundle bundle = fhirClientService.search(Organization.class, getPracticeId());
        List<Organization> orgs = fhirClientService.extractResources(bundle, Organization.class);

        // Filter to referral practices (type = prov)
        return orgs.stream()
                .filter(this::isReferralPractice)
                .map(this::fromFhirOrganization)
                .collect(Collectors.toList());
    }

    // UPDATE
    public ReferralPracticeDto update(String fhirId, ReferralPracticeDto dto) {
        log.debug("Updating FHIR Organization (referral practice): {}", fhirId);

        Organization org = toFhirOrganization(dto);
        org.setId(fhirId);
        fhirClientService.update(org, getPracticeId());

        dto.setFhirId(fhirId);
        return dto;
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting FHIR Organization (referral practice): {}", fhirId);
        fhirClientService.delete(Organization.class, fhirId, getPracticeId());
    }

    // -------- FHIR Mapping --------

    private Organization toFhirOrganization(ReferralPracticeDto dto) {
        Organization org = new Organization();
        org.setActive(true);

        // Type = healthcare provider (referral practice)
        org.addType().addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/organization-type")
                .setCode("prov")
                .setDisplay("Healthcare Provider");

        // Name
        if (dto.getName() != null) {
            org.setName(dto.getName());
        }

        // Address
        Address address = org.addAddress();
        if (dto.getAddress() != null) address.addLine(dto.getAddress());
        if (dto.getCity() != null) address.setCity(dto.getCity());
        if (dto.getState() != null) address.setState(dto.getState());
        if (dto.getPostalCode() != null) address.setPostalCode(dto.getPostalCode());
        if (dto.getCountry() != null) address.setCountry(dto.getCountry());

        // Telecom
        if (dto.getPhoneNumber() != null) {
            org.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(dto.getPhoneNumber());
        }
        if (dto.getEmail() != null) {
            org.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(dto.getEmail());
        }

        // Extensions
        if (dto.getNpiId() != null) {
            org.addExtension(new Extension(EXT_NPI_ID, new StringType(dto.getNpiId())));
        }
        if (dto.getTaxId() != null) {
            org.addExtension(new Extension(EXT_TAX_ID, new StringType(dto.getTaxId())));
        }

        return org;
    }

    private ReferralPracticeDto fromFhirOrganization(Organization org) {
        ReferralPracticeDto dto = new ReferralPracticeDto();
        dto.setFhirId(org.getIdElement().getIdPart());
        dto.setId(Long.parseLong(org.getIdElement().getIdPart()));

        // Name
        if (org.hasName()) {
            dto.setName(org.getName());
        }

        // Address
        if (org.hasAddress()) {
            Address addr = org.getAddressFirstRep();
            if (addr.hasLine()) dto.setAddress(addr.getLine().get(0).getValue());
            if (addr.hasCity()) dto.setCity(addr.getCity());
            if (addr.hasState()) dto.setState(addr.getState());
            if (addr.hasPostalCode()) dto.setPostalCode(addr.getPostalCode());
            if (addr.hasCountry()) dto.setCountry(addr.getCountry());
        }

        // Telecom
        for (ContactPoint cp : org.getTelecom()) {
            if (cp.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                dto.setPhoneNumber(cp.getValue());
            } else if (cp.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                dto.setEmail(cp.getValue());
            }
        }

        // Extensions
        dto.setNpiId(getExtensionString(org, EXT_NPI_ID));
        dto.setTaxId(getExtensionString(org, EXT_TAX_ID));

        // Set audit information
        ReferralPracticeDto.Audit audit = new ReferralPracticeDto.Audit();
        audit.setCreatedDate(java.time.LocalDateTime.now().toString());
        audit.setLastModifiedDate(java.time.LocalDateTime.now().toString());
        dto.setAudit(audit);

        return dto;
    }

    private boolean isReferralPractice(Organization org) {
        if (!org.hasType()) return false;
        return org.getType().stream()
                .flatMap(cc -> cc.getCoding().stream())
                .anyMatch(c -> "prov".equals(c.getCode()));
    }

    private String getExtensionString(Organization org, String url) {
        Extension ext = org.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }

    private void validateMandatoryFields(ReferralPracticeDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Missing mandatory field: name");
        }
    }
}
