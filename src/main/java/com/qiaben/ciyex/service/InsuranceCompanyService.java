package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.InsuranceCompanyDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only InsuranceCompany Service.
 * Uses FHIR Organization (type: ins) resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InsuranceCompanyService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // Extension URLs
    private static final String EXT_PAYER_ID = "http://ciyex.com/fhir/StructureDefinition/payer-id";
    private static final String EXT_STATUS = "http://ciyex.com/fhir/StructureDefinition/insurance-status";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public InsuranceCompanyDto create(InsuranceCompanyDto dto) {
        validateMandatoryFields(dto);

        log.debug("Creating FHIR Organization (insurance): {}", dto.getName());

        Organization org = toFhirOrganization(dto);
        var outcome = fhirClientService.create(org, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        log.info("Created FHIR Organization (insurance) with id: {}", fhirId);

        return dto;
    }

    // GET BY ID
    public InsuranceCompanyDto getById(String fhirId) {
        log.debug("Getting FHIR Organization (insurance): {}", fhirId);
        Organization org = fhirClientService.read(Organization.class, fhirId, getPracticeId());
        return fromFhirOrganization(org);
    }

    // GET ALL
    public List<InsuranceCompanyDto> getAll() {
        log.debug("Getting all FHIR Organizations (insurance)");

        Bundle bundle = fhirClientService.search(Organization.class, getPracticeId());
        List<Organization> orgs = fhirClientService.extractResources(bundle, Organization.class);

        // Filter by type = ins
        return orgs.stream()
                .filter(this::isInsuranceOrg)
                .map(this::fromFhirOrganization)
                .collect(Collectors.toList());
    }

    // UPDATE
    public InsuranceCompanyDto update(String fhirId, InsuranceCompanyDto dto) {
        log.debug("Updating FHIR Organization (insurance): {}", fhirId);

        Organization org = toFhirOrganization(dto);
        org.setId(fhirId);
        fhirClientService.update(org, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        return dto;
    }

    // UPDATE STATUS
    public InsuranceCompanyDto updateStatus(String fhirId, String status) {
        log.debug("Updating status for FHIR Organization (insurance): {} to {}", fhirId, status);

        Organization org = fhirClientService.read(Organization.class, fhirId, getPracticeId());
        
        // Remove existing status extension and add new one
        org.getExtension().removeIf(e -> EXT_STATUS.equals(e.getUrl()));
        org.addExtension(new Extension(EXT_STATUS, new StringType(status)));
        
        fhirClientService.update(org, getPracticeId());
        return fromFhirOrganization(org);
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting FHIR Organization (insurance): {}", fhirId);
        fhirClientService.delete(Organization.class, fhirId, getPracticeId());
    }

    // -------- FHIR Mapping --------

    private Organization toFhirOrganization(InsuranceCompanyDto dto) {
        Organization org = new Organization();
        org.setActive(true);

        // Type = insurance
        org.addType().addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/organization-type")
                .setCode("ins")
                .setDisplay("Insurance Company");

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

        // Payer ID extension
        if (dto.getPayerId() != null) {
            org.addExtension(new Extension(EXT_PAYER_ID, new StringType(dto.getPayerId())));
        }

        // Status extension
        String status = dto.getStatus() != null ? dto.getStatus() : "ACTIVE";
        org.addExtension(new Extension(EXT_STATUS, new StringType(status)));

        return org;
    }

    private InsuranceCompanyDto fromFhirOrganization(Organization org) {
        InsuranceCompanyDto dto = new InsuranceCompanyDto();
        dto.setFhirId(org.getIdElement().getIdPart());
        dto.setExternalId(org.getIdElement().getIdPart());

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

        // Payer ID extension
        Extension payerExt = org.getExtensionByUrl(EXT_PAYER_ID);
        if (payerExt != null && payerExt.getValue() instanceof StringType) {
            dto.setPayerId(((StringType) payerExt.getValue()).getValue());
        }

        // Status extension
        Extension statusExt = org.getExtensionByUrl(EXT_STATUS);
        if (statusExt != null && statusExt.getValue() instanceof StringType) {
            dto.setStatus(((StringType) statusExt.getValue()).getValue());
        } else {
            dto.setStatus("ACTIVE");
        }

        return dto;
    }

    private boolean isInsuranceOrg(Organization org) {
        if (!org.hasType()) return false;
        return org.getType().stream()
                .flatMap(cc -> cc.getCoding().stream())
                .anyMatch(c -> "ins".equals(c.getCode()));
    }

    // -------- Validation --------

    private void validateMandatoryFields(InsuranceCompanyDto dto) {
        StringBuilder errors = new StringBuilder();

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            errors.append("Name is mandatory. ");
        }
        if (dto.getAddress() == null || dto.getAddress().trim().isEmpty()) {
            errors.append("Address is mandatory. ");
        }
        if (dto.getCity() == null || dto.getCity().trim().isEmpty()) {
            errors.append("City is mandatory. ");
        }
        if (dto.getState() == null || dto.getState().trim().isEmpty()) {
            errors.append("State is mandatory. ");
        }
        if (dto.getPostalCode() == null || dto.getPostalCode().trim().isEmpty()) {
            errors.append("Postal code is mandatory. ");
        }
        if (dto.getPayerId() == null || dto.getPayerId().trim().isEmpty()) {
            errors.append("Payer ID is mandatory. ");
        }
        if (dto.getCountry() == null || dto.getCountry().trim().isEmpty()) {
            errors.append("Country is mandatory. ");
        }

        if (errors.length() > 0) {
            throw new IllegalArgumentException(errors.toString().trim());
        }
    }
}
