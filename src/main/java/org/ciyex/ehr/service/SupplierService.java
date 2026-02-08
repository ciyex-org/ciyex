package org.ciyex.ehr.service;

import org.ciyex.ehr.dto.SupplierDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only Supplier Service.
 * Uses FHIR Organization (type=supplier) resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public SupplierDto create(SupplierDto dto) {
        log.debug("Creating FHIR Organization (supplier): {}", dto.getName());

        Organization org = toFhirOrganization(dto);
        var outcome = fhirClientService.create(org, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setAudit(createAudit());
        log.info("Created FHIR Organization (supplier) with id: {}", fhirId);

        return dto;
    }

    // GET BY ID
    public SupplierDto getById(String fhirId) {
        log.debug("Getting FHIR Organization (supplier): {}", fhirId);
        Organization org = fhirClientService.read(Organization.class, fhirId, getPracticeId());
        return fromFhirOrganization(org);
    }

    // GET ALL
    public List<SupplierDto> getAll() {
        log.debug("Getting all FHIR Organizations (supplier)");

        Bundle bundle = fhirClientService.search(Organization.class, getPracticeId());
        List<Organization> orgs = fhirClientService.extractResources(bundle, Organization.class);

        return orgs.stream()
                .filter(this::isSupplierOrg)
                .map(this::fromFhirOrganization)
                .collect(Collectors.toList());
    }

    // GET ALL (Paginated)
    public Page<SupplierDto> getAll(Pageable pageable) {
        List<SupplierDto> all = getAll();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<SupplierDto> pageContent = all.subList(start, end);
        return new PageImpl<>(pageContent, pageable, all.size());
    }

    // UPDATE
    public SupplierDto update(String fhirId, SupplierDto dto) {
        log.debug("Updating FHIR Organization (supplier): {}", fhirId);

        Organization org = toFhirOrganization(dto);
        org.setId(fhirId);
        fhirClientService.update(org, getPracticeId());

        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setAudit(createAudit());
        return dto;
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting FHIR Organization (supplier): {}", fhirId);
        fhirClientService.read(Organization.class, fhirId, getPracticeId());
        fhirClientService.delete(Organization.class, fhirId, getPracticeId());
    }

    // COUNT
    public Long countByOrg() {
        return (long) getAll().size();
    }

    // -------- FHIR Mapping --------

    private Organization toFhirOrganization(SupplierDto dto) {
        Organization org = new Organization();
        org.setActive(true);

        // Type = supplier
        CodeableConcept type = new CodeableConcept();
        type.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/organization-type")
                .setCode("bus")
                .setDisplay("Non-Healthcare Business Corporation");
        type.setText("supplier");
        org.addType(type);

        // Name
        if (dto.getName() != null) {
            org.setName(dto.getName());
        }

        // Telecom
        if (dto.getPhone() != null) {
            org.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(dto.getPhone());
        }
        if (dto.getEmail() != null) {
            org.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(dto.getEmail());
        }

        return org;
    }

    private SupplierDto fromFhirOrganization(Organization org) {
        SupplierDto dto = new SupplierDto();
        String fhirId = org.getIdElement().getIdPart();
        
        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        // Name
        if (org.hasName()) {
            dto.setName(org.getName());
        }

        // Telecom
        for (ContactPoint cp : org.getTelecom()) {
            if (cp.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                dto.setPhone(cp.getValue());
            } else if (cp.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                dto.setEmail(cp.getValue());
            }
        }

        dto.setAudit(createAudit());
        return dto;
    }

    private boolean isSupplierOrg(Organization org) {
        if (!org.hasType()) return false;
        return org.getType().stream()
                .anyMatch(cc -> "supplier".equalsIgnoreCase(cc.getText()));
    }

    private SupplierDto.Audit createAudit() {
        SupplierDto.Audit audit = new SupplierDto.Audit();
        String currentTime = java.time.LocalDateTime.now().toString();
        audit.setCreatedDate(currentTime);
        audit.setLastModifiedDate(currentTime);
        return audit;
    }
}
