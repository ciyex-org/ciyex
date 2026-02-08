package org.ciyex.ehr.service;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import org.ciyex.ehr.dto.ReferralProviderDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only ReferralProvider Service.
 * Uses FHIR Practitioner resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralProviderService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // Extension URLs
    private static final String EXT_SPECIALTY = "http://ciyex.com/fhir/StructureDefinition/specialty";
    private static final String EXT_NPI_ID = "http://ciyex.com/fhir/StructureDefinition/npi-id";
    private static final String EXT_TAX_ID = "http://ciyex.com/fhir/StructureDefinition/tax-id";
    private static final String EXT_PRACTICE_ID = "http://ciyex.com/fhir/StructureDefinition/practice-id";
    private static final String EXT_PRACTICE_NAME = "http://ciyex.com/fhir/StructureDefinition/practice-name";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public ReferralProviderDto create(ReferralProviderDto dto) {
        validateMandatoryFields(dto);
        
        if (dto.getPracticeId() == null && (dto.getPractice() == null || dto.getPractice().getId() == null)) {
            throw new IllegalArgumentException("Practice id is required");
        }

        log.debug("Creating FHIR Practitioner (referral): {}", dto.getName());

        Practitioner pract = toFhirPractitioner(dto);
        var outcome = fhirClientService.create(pract, getPracticeId());
        String fhirId = outcome.getId().getIdPart();
        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);

        // Set audit information
        ReferralProviderDto.Audit audit = new ReferralProviderDto.Audit();
        audit.setCreatedDate(java.time.LocalDateTime.now().toString());
        audit.setLastModifiedDate(java.time.LocalDateTime.now().toString());
        dto.setAudit(audit);

        log.info("Created FHIR Practitioner (referral) with id: {}", fhirId);

        return dto;
    }

    // GET BY ID
    public ReferralProviderDto getById(String fhirId) {
        log.debug("Getting FHIR Practitioner (referral): {}", fhirId);
        try {
            Practitioner pract = fhirClientService.read(Practitioner.class, fhirId, getPracticeId());
            return fromFhirPractitioner(pract);
        } catch (ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException e) {
            throw new IllegalArgumentException("Referral provider with FHIR ID " + fhirId + " not found");
        } catch (Exception e) {
            log.error("Error retrieving referral provider {}: {}", fhirId, e.getMessage());
            throw new RuntimeException("Failed to retrieve referral provider: " + e.getMessage(), e);
        }
    }

    // GET BY ID WITH PRACTICE (alias for getById)
    public ReferralProviderDto getByIdWithPractice(String fhirId) {
        return getById(fhirId);
    }

    // GET ALL
    public List<ReferralProviderDto> getAll() {
        log.debug("Getting all FHIR Practitioners (referral)");

        Bundle bundle = fhirClientService.search(Practitioner.class, getPracticeId());
        List<Practitioner> practitioners = fhirClientService.extractResources(bundle, Practitioner.class);

        // Filter to only referral practitioners (those with practice extension)
        return practitioners.stream()
                .filter(p -> p.hasExtension(EXT_PRACTICE_ID))
                .map(this::fromFhirPractitioner)
                .collect(Collectors.toList());
    }

    // GET BY PRACTICE ID
    public List<ReferralProviderDto> getByPracticeId(Long practiceId) {
        log.debug("Getting FHIR Practitioners for practice: {}", practiceId);

        Bundle bundle = fhirClientService.search(Practitioner.class, getPracticeId());
        List<Practitioner> practitioners = fhirClientService.extractResources(bundle, Practitioner.class);

        return practitioners.stream()
                .filter(p -> hasPracticeId(p, practiceId))
                .map(this::fromFhirPractitioner)
                .collect(Collectors.toList());
    }

    // UPDATE
    public ReferralProviderDto update(String fhirId, ReferralProviderDto dto) {
        log.debug("Updating FHIR Practitioner (referral): {}", fhirId);

        Practitioner pract = toFhirPractitioner(dto);
        pract.setId(fhirId);
        fhirClientService.update(pract, getPracticeId());

        dto.setFhirId(fhirId);
        return dto;
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting FHIR Practitioner (referral): {}", fhirId);
        fhirClientService.delete(Practitioner.class, fhirId, getPracticeId());
    }

    // -------- FHIR Mapping --------

    private Practitioner toFhirPractitioner(ReferralProviderDto dto) {
        Practitioner p = new Practitioner();
        p.setActive(true);

        // Name - support both firstName/lastName and name field
        HumanName humanName = p.addName();
        if (dto.getFirstName() != null || dto.getLastName() != null) {
            if (dto.getFirstName() != null) humanName.addGiven(dto.getFirstName());
            if (dto.getLastName() != null) humanName.setFamily(dto.getLastName());
            humanName.setText((dto.getFirstName() != null ? dto.getFirstName() : "") + 
                             (dto.getLastName() != null ? " " + dto.getLastName() : "").trim());
        } else if (dto.getName() != null) {
            humanName.setText(dto.getName());
        }

        // Address
        Address address = p.addAddress();
        if (dto.getAddress() != null) address.addLine(dto.getAddress());
        if (dto.getCity() != null) address.setCity(dto.getCity());
        if (dto.getState() != null) address.setState(dto.getState());
        if (dto.getPostalCode() != null) address.setPostalCode(dto.getPostalCode());
        if (dto.getCountry() != null) address.setCountry(dto.getCountry());

        // Telecom
        if (dto.getPhoneNumber() != null) {
            p.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(dto.getPhoneNumber());
        }
        if (dto.getEmail() != null) {
            p.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(dto.getEmail());
        }

        // Extensions
        if (dto.getSpecialty() != null) {
            p.addExtension(new Extension(EXT_SPECIALTY, new StringType(dto.getSpecialty())));
        }
        if (dto.getNpiId() != null) {
            p.addExtension(new Extension(EXT_NPI_ID, new StringType(dto.getNpiId())));
        }
        if (dto.getTaxId() != null) {
            p.addExtension(new Extension(EXT_TAX_ID, new StringType(dto.getTaxId())));
        }

        // Practice reference
        Long practiceId = dto.getPracticeId() != null ? dto.getPracticeId()
                : (dto.getPractice() != null ? dto.getPractice().getId() : null);
        if (practiceId != null) {
            p.addExtension(new Extension(EXT_PRACTICE_ID, new StringType(practiceId.toString())));
        }
        if (dto.getPractice() != null && dto.getPractice().getName() != null) {
            p.addExtension(new Extension(EXT_PRACTICE_NAME, new StringType(dto.getPractice().getName())));
        }

        return p;
    }

    private ReferralProviderDto fromFhirPractitioner(Practitioner p) {
        ReferralProviderDto dto = new ReferralProviderDto();
        dto.setFhirId(p.getIdElement().getIdPart());
        dto.setId(Long.parseLong(p.getIdElement().getIdPart()));

        // Name
        if (p.hasName()) {
            HumanName humanName = p.getNameFirstRep();
            if (humanName.hasGiven()) dto.setFirstName(humanName.getGivenAsSingleString());
            if (humanName.hasFamily()) dto.setLastName(humanName.getFamily());
            if (humanName.hasText()) dto.setName(humanName.getText());
        }

        // Address
        if (p.hasAddress()) {
            Address addr = p.getAddressFirstRep();
            if (addr.hasLine()) dto.setAddress(addr.getLine().get(0).getValue());
            if (addr.hasCity()) dto.setCity(addr.getCity());
            if (addr.hasState()) dto.setState(addr.getState());
            if (addr.hasPostalCode()) dto.setPostalCode(addr.getPostalCode());
            if (addr.hasCountry()) dto.setCountry(addr.getCountry());
        }

        // Telecom
        for (ContactPoint cp : p.getTelecom()) {
            if (cp.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                dto.setPhoneNumber(cp.getValue());
            } else if (cp.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                dto.setEmail(cp.getValue());
            }
        }

        // Extensions
        dto.setSpecialty(getExtensionString(p, EXT_SPECIALTY));
        dto.setNpiId(getExtensionString(p, EXT_NPI_ID));
        dto.setTaxId(getExtensionString(p, EXT_TAX_ID));

        // Practice
        String practiceIdStr = getExtensionString(p, EXT_PRACTICE_ID);
        String practiceName = getExtensionString(p, EXT_PRACTICE_NAME);
        if (practiceIdStr != null) {
            try {
                Long practiceId = Long.parseLong(practiceIdStr);
                dto.setPracticeId(practiceId);
                ReferralProviderDto.PracticeInfo pi = new ReferralProviderDto.PracticeInfo();
                pi.setId(practiceId);
                pi.setName(practiceName);
                dto.setPractice(pi);
            } catch (NumberFormatException ignored) {}
        }

        // Set audit information
        ReferralProviderDto.Audit audit = new ReferralProviderDto.Audit();
        audit.setCreatedDate(java.time.LocalDateTime.now().toString());
        audit.setLastModifiedDate(java.time.LocalDateTime.now().toString());
        dto.setAudit(audit);

        return dto;
    }

    // -------- Helpers --------

    private String getExtensionString(Practitioner p, String url) {
        Extension ext = p.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }

    private boolean hasPracticeId(Practitioner p, Long practiceId) {
        String practiceIdStr = getExtensionString(p, EXT_PRACTICE_ID);
        return practiceIdStr != null && practiceIdStr.equals(practiceId.toString());
    }

    private void validateMandatoryFields(ReferralProviderDto dto) {
        if (dto.getFirstName() == null || dto.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("Missing mandatory field: firstName");
        }
        if (dto.getNpiId() == null || dto.getNpiId().trim().isEmpty()) {
            throw new IllegalArgumentException("Missing mandatory field: npiId");
        }
    }
}
