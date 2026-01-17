package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import com.qiaben.ciyex.dto.PatientRelationshipDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only PatientRelationship Service.
 * Uses FHIR RelatedPerson resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientRelationshipService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // Extension URLs
    private static final String EXT_RELATED_PATIENT_ID = "http://ciyex.com/fhir/StructureDefinition/related-patient-id";
    private static final String EXT_EMERGENCY_CONTACT = "http://ciyex.com/fhir/StructureDefinition/emergency-contact";
    private static final String EXT_NOTES = "http://ciyex.com/fhir/StructureDefinition/notes";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public PatientRelationshipDto create(PatientRelationshipDto dto) {
        log.debug("Creating FHIR RelatedPerson for patient: {}", dto.getPatientId());

        // Validate patient exists
        validatePatientExists(dto.getPatientId());

        RelatedPerson rp = toFhirRelatedPerson(dto);
        var outcome = fhirClientService.create(rp, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId(Long.parseLong(fhirId));
        
        // Set audit information
        LocalDateTime currentTime = LocalDateTime.now();
        dto.setCreatedDate(currentTime);
        dto.setLastModifiedDate(currentTime);
        
        log.info("Created FHIR RelatedPerson with id: {}", fhirId);

        return dto;
    }

    // GET BY ID
    public PatientRelationshipDto getById(String fhirId) {
        log.debug("Getting FHIR RelatedPerson: {}", fhirId);
        try {
            RelatedPerson rp = fhirClientService.read(RelatedPerson.class, fhirId, getPracticeId());
            if (rp == null) {
                throw new RuntimeException("Relationship with ID " + fhirId + " not found");
            }
            return fromFhirRelatedPerson(rp);
        } catch (Exception e) {
            throw new RuntimeException("Relationship with ID " + fhirId + " not found");
        }
    }

    // GET ALL BY PATIENT
    public List<PatientRelationshipDto> getAllByPatientId(Long patientId) {
        log.debug("Getting FHIR RelatedPersons for patient: {}", patientId);

        // Validate patient exists
        validatePatientExists(patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(RelatedPerson.class)
                .where(new ReferenceClientParam("patient").hasId("Patient/" + patientId))
                .returnBundle(Bundle.class)
                .execute();

        List<RelatedPerson> relatedPersons = fhirClientService.extractResources(bundle, RelatedPerson.class);
        return relatedPersons.stream().map(this::fromFhirRelatedPerson).collect(Collectors.toList());
    }

    // UPDATE
    public PatientRelationshipDto update(String fhirId, PatientRelationshipDto dto) {
        log.debug("Updating FHIR RelatedPerson: {}", fhirId);

        // Validate relationship exists
        getById(fhirId);
        
        // Validate patient exists
        validatePatientExists(dto.getPatientId());

        RelatedPerson rp = toFhirRelatedPerson(dto);
        rp.setId(fhirId);
        fhirClientService.update(rp, getPracticeId());

        dto.setId(Long.parseLong(fhirId));
        
        // Set audit information - preserve createdDate, update lastModifiedDate
        if (dto.getCreatedDate() == null) {
            dto.setCreatedDate(LocalDateTime.now());
        }
        dto.setLastModifiedDate(LocalDateTime.now());

        return dto;
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting FHIR RelatedPerson: {}", fhirId);
        
        // Validate relationship exists
        getById(fhirId);
        
        fhirClientService.delete(RelatedPerson.class, fhirId, getPracticeId());
    }

    // VALIDATION
    private void validatePatientExists(Long patientId) {
        try {
            fhirClientService.read(Patient.class, patientId.toString(), getPracticeId());
        } catch (Exception e) {
            throw new RuntimeException("Patient with ID " + patientId + " not found");
        }
    }

    // -------- FHIR Mapping --------

    private RelatedPerson toFhirRelatedPerson(PatientRelationshipDto dto) {
        RelatedPerson rp = new RelatedPerson();
        rp.setActive(dto.getActive() != null ? dto.getActive() : true);

        // Patient reference
        if (dto.getPatientId() != null) {
            rp.setPatient(new Reference("Patient/" + dto.getPatientId()));
        }

        // Name
        if (dto.getRelatedPatientName() != null) {
            rp.addName().setText(dto.getRelatedPatientName());
        }

        // Relationship type
        if (dto.getRelationshipType() != null) {
            rp.addRelationship().setText(dto.getRelationshipType());
        }

        // Telecom
        if (dto.getPhoneNumber() != null) {
            rp.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(dto.getPhoneNumber());
        }
        if (dto.getEmail() != null) {
            rp.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(dto.getEmail());
        }

        // Address
        if (dto.getAddress() != null) {
            rp.addAddress().setText(dto.getAddress());
        }

        // Extensions
        if (dto.getRelatedPatientId() != null) {
            rp.addExtension(new Extension(EXT_RELATED_PATIENT_ID, new StringType(dto.getRelatedPatientId().toString())));
        }
        if (dto.getEmergencyContact() != null) {
            rp.addExtension(new Extension(EXT_EMERGENCY_CONTACT, new BooleanType(dto.getEmergencyContact())));
        }
        if (dto.getNotes() != null) {
            rp.addExtension(new Extension(EXT_NOTES, new StringType(dto.getNotes())));
        }

        return rp;
    }

    private PatientRelationshipDto fromFhirRelatedPerson(RelatedPerson rp) {
        PatientRelationshipDto dto = PatientRelationshipDto.builder().build();
        
        String fhirId = rp.getIdElement().getIdPart();
        dto.setId(Long.parseLong(fhirId));
        
        // Set audit information
        LocalDateTime currentTime = LocalDateTime.now();
        dto.setCreatedDate(currentTime);
        dto.setLastModifiedDate(currentTime);

        // Patient -> patientId
        if (rp.hasPatient() && rp.getPatient().hasReference()) {
            String ref = rp.getPatient().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    dto.setPatientId(Long.parseLong(ref.substring("Patient/".length())));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Active
        dto.setActive(rp.getActive());

        // Name
        if (rp.hasName()) {
            dto.setRelatedPatientName(rp.getNameFirstRep().getText());
        }

        // Relationship type
        if (rp.hasRelationship()) {
            dto.setRelationshipType(rp.getRelationshipFirstRep().getText());
        }

        // Telecom
        for (ContactPoint cp : rp.getTelecom()) {
            if (cp.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                dto.setPhoneNumber(cp.getValue());
            } else if (cp.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                dto.setEmail(cp.getValue());
            }
        }

        // Address
        if (rp.hasAddress()) {
            dto.setAddress(rp.getAddressFirstRep().getText());
        }

        // Extensions
        Extension relatedIdExt = rp.getExtensionByUrl(EXT_RELATED_PATIENT_ID);
        if (relatedIdExt != null && relatedIdExt.getValue() instanceof StringType) {
            try {
                dto.setRelatedPatientId(Long.parseLong(((StringType) relatedIdExt.getValue()).getValue()));
            } catch (NumberFormatException ignored) {}
        }

        Extension emergencyExt = rp.getExtensionByUrl(EXT_EMERGENCY_CONTACT);
        if (emergencyExt != null && emergencyExt.getValue() instanceof BooleanType) {
            dto.setEmergencyContact(((BooleanType) emergencyExt.getValue()).booleanValue());
        }

        Extension notesExt = rp.getExtensionByUrl(EXT_NOTES);
        if (notesExt != null && notesExt.getValue() instanceof StringType) {
            dto.setNotes(((StringType) notesExt.getValue()).getValue());
        }

        return dto;
    }
}
