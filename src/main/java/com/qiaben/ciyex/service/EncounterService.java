package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.qiaben.ciyex.dto.EncounterDto;
import com.qiaben.ciyex.dto.EncounterStatus;
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
 * Encounter Service - FHIR Only.
 * All encounter data is stored in HAPI FHIR server as Encounter resources.
 */
@Service
@Slf4j
public class EncounterService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public EncounterService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // ✅ Create encounter in FHIR
    public EncounterDto createEncounter(Long patientId, EncounterDto dto) {
        String patientFhirId = String.valueOf(patientId);
        log.info("Creating encounter in FHIR for patient: {}", patientFhirId);
        
        Encounter fhirEncounter = toFhirEncounter(dto, patientFhirId);
        
        MethodOutcome outcome = fhirClientService.create(fhirEncounter, getPracticeId());
        
        String fhirId = outcome.getId().getIdPart();
        Encounter created = fhirClientService.read(Encounter.class, fhirId, getPracticeId());
        
        return toEncounterDto(created);
    }

    // ✅ List encounters by patient
    public List<EncounterDto> listByPatient(Long patientId) {
        String patientFhirId = String.valueOf(patientId);
        log.debug("Listing FHIR Encounters for patient: {}", patientFhirId);
        
        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Encounter.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientFhirId))
                
                .returnBundle(Bundle.class)
                .execute();
        
        return extractEncounters(bundle);
    }

    // ✅ Get encounter by FHIR ID for patient
    public EncounterDto getByIdForPatient(Long id, Long patientId) {
        return getByFhirId(String.valueOf(id));
    }

    public EncounterDto getByFhirId(String fhirId) {
        log.debug("Reading FHIR Encounter with ID: {}", fhirId);
        try {
            Encounter fhirEncounter = fhirClientService.read(Encounter.class, fhirId, getPracticeId());
            return toEncounterDto(fhirEncounter);
        } catch (ResourceNotFoundException e) {
            throw new IllegalArgumentException("Encounter not found with FHIR ID: " + fhirId);
        }
    }

    // ✅ Update encounter in FHIR
    public EncounterDto updateEncounter(Long id, Long patientId, EncounterDto dto) {
        String fhirId = String.valueOf(id);
        String patientFhirId = String.valueOf(patientId);
        
        // Check if signed
        EncounterDto existing = getByFhirId(fhirId);
        if (existing.getStatus() == EncounterStatus.SIGNED) {
            throw new IllegalStateException("Cannot modify data for a signed encounter. Please unsign the encounter first.");
        }
        
        log.info("Updating FHIR Encounter with ID: {}", fhirId);
        
        Encounter fhirEncounter = toFhirEncounter(dto, patientFhirId);
        fhirEncounter.setId(fhirId);
        
        fhirClientService.update(fhirEncounter, getPracticeId());
        
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        
        log.info("Updated FHIR Encounter with ID: {}", fhirId);
        return dto;
    }

    // ✅ Delete encounter from FHIR
    public void deleteEncounter(Long id, Long patientId) {
        String fhirId = String.valueOf(id);
        
        // Check if signed
        EncounterDto existing = getByFhirId(fhirId);
        if (existing.getStatus() == EncounterStatus.SIGNED) {
            throw new IllegalStateException("Cannot delete a signed encounter. Please unsign the encounter first.");
        }
        
        log.info("Deleting FHIR Encounter with ID: {}", fhirId);
        fhirClientService.delete(Encounter.class, fhirId, getPracticeId());
        log.info("Deleted FHIR Encounter with ID: {}", fhirId);
    }

    // ✅ Sign encounter
    public EncounterDto signEncounter(Long id, Long patientId) {
        return updateEncounterStatus(id, patientId, EncounterStatus.SIGNED);
    }

    // ✅ Unsign encounter
    public EncounterDto unsignEncounter(Long id, Long patientId) {
        return updateEncounterStatus(id, patientId, EncounterStatus.UNSIGNED);
    }

    // ✅ Mark incomplete
    public EncounterDto markIncomplete(Long id, Long patientId) {
        return updateEncounterStatus(id, patientId, EncounterStatus.INCOMPLETE);
    }

    private EncounterDto updateEncounterStatus(Long id, Long patientId, EncounterStatus status) {
        String fhirId = String.valueOf(id);
        log.info("Updating status for FHIR Encounter with ID: {} to {}", fhirId, status);
        
        try {
            Encounter fhirEncounter = fhirClientService.read(Encounter.class, fhirId, getPracticeId());
            fhirEncounter.setStatus(mapToFhirStatus(status));
            fhirClientService.update(fhirEncounter, getPracticeId());
            return toEncounterDto(fhirEncounter);
        } catch (ResourceNotFoundException e) {
            throw new IllegalArgumentException("Encounter not found with FHIR ID: " + fhirId);
        }
    }

    // ✅ Validate encounter not signed
    public void validateEncounterNotSigned(Long encounterId, Long patientId) {
        EncounterDto dto = getByFhirId(String.valueOf(encounterId));
        if (dto.getStatus() == EncounterStatus.SIGNED) {
            throw new IllegalStateException("Cannot modify data for a signed encounter. Please unsign the encounter first.");
        }
    }

    // ✅ Get encounter status
    public EncounterStatus getEncounterStatus(Long encounterId, Long patientId) {
        EncounterDto dto = getByFhirId(String.valueOf(encounterId));
        return dto.getStatus() != null ? dto.getStatus() : EncounterStatus.UNSIGNED;
    }

    // ========== FHIR Mapping Methods ==========

    private Encounter toFhirEncounter(EncounterDto dto, String patientFhirId) {
        Encounter encounter = new Encounter();

        // Status
        encounter.setStatus(mapToFhirStatus(dto.getStatus()));

        // Class - ambulatory by default
        encounter.setClass_(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                .setCode("AMB")
                .setDisplay("ambulatory"));

        // Subject (Patient reference)
        encounter.setSubject(new Reference("Patient/" + patientFhirId));

        // Type
        if (dto.getType() != null) {
            encounter.addType()
                    .addCoding()
                    .setSystem("http://snomed.info/sct")
                    .setDisplay(dto.getType());
        }

        // Visit Category
        if (dto.getVisitCategory() != null) {
            encounter.addType()
                    .setText(dto.getVisitCategory());
        }

        // Reason for visit
        if (dto.getReasonForVisit() != null) {
            encounter.addReasonCode()
                    .setText(dto.getReasonForVisit());
        }

        // Provider (Participant)
        if (dto.getEncounterProvider() != null) {
            encounter.addParticipant()
                    .setIndividual(new Reference().setDisplay(dto.getEncounterProvider()));
        }

        // Encounter date (Period)
        if (dto.getEncounterDate() != null) {
            Period period = new Period();
            Date encounterDate = Date.from(dto.getEncounterDate().atZone(ZoneId.systemDefault()).toInstant());
            period.setStart(encounterDate);
            encounter.setPeriod(period);
        }

        // Discharge disposition
        if (dto.getDischargeDisposition() != null) {
            encounter.getHospitalization()
                    .getDischargeDisposition()
                    .setText(dto.getDischargeDisposition());
        }

        // Sensitivity
        if (dto.getSensitivity() != null) {
            encounter.getMeta().addSecurity()
                    .setSystem("http://terminology.hl7.org/CodeSystem/v3-Confidentiality")
                    .setCode(dto.getSensitivity());
        }

        return encounter;
    }

    private EncounterDto toEncounterDto(Encounter encounter) {
        EncounterDto dto = new EncounterDto();

        // FHIR ID
        if (encounter.hasId()) {
            String idPart = encounter.getIdElement().getIdPart();
            dto.setFhirId(idPart);
            dto.setExternalId(idPart);
            try {
                dto.setId(Long.parseLong(idPart));
            } catch (NumberFormatException e) {
                // FHIR ID is not numeric
            }
        }

        // Status
        dto.setStatus(mapFromFhirStatus(encounter.getStatus()));

        // Patient ID from subject reference
        if (encounter.hasSubject() && encounter.getSubject().hasReference()) {
            String ref = encounter.getSubject().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    dto.setPatientId(Long.parseLong(ref.substring(8)));
                } catch (NumberFormatException e) {
                    // FHIR ID is not numeric
                }
            }
        }

        // Type and Visit Category
        if (encounter.hasType()) {
            for (CodeableConcept type : encounter.getType()) {
                if (type.hasCoding()) {
                    dto.setType(type.getCodingFirstRep().getDisplay());
                } else if (type.hasText()) {
                    dto.setVisitCategory(type.getText());
                }
            }
        }

        // Reason for visit
        if (encounter.hasReasonCode()) {
            dto.setReasonForVisit(encounter.getReasonCodeFirstRep().getText());
        }

        // Provider
        if (encounter.hasParticipant()) {
            Encounter.EncounterParticipantComponent participant = encounter.getParticipantFirstRep();
            if (participant.hasIndividual() && participant.getIndividual().hasDisplay()) {
                dto.setEncounterProvider(participant.getIndividual().getDisplay());
            }
        }

        // Encounter date
        if (encounter.hasPeriod() && encounter.getPeriod().hasStart()) {
            dto.setEncounterDate(encounter.getPeriod().getStart().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());
        }

        // Discharge disposition
        if (encounter.hasHospitalization() && 
            encounter.getHospitalization().hasDischargeDisposition()) {
            dto.setDischargeDisposition(encounter.getHospitalization().getDischargeDisposition().getText());
        }

        // Sensitivity
        if (encounter.hasMeta() && encounter.getMeta().hasSecurity()) {
            dto.setSensitivity(encounter.getMeta().getSecurityFirstRep().getCode());
        }

        // Audit fields from FHIR metadata
        if (encounter.hasMeta()) {
            EncounterDto.Audit audit = new EncounterDto.Audit();
            if (encounter.getMeta().hasLastUpdated()) {
                audit.setCreatedDate(formatDate(encounter.getMeta().getLastUpdated()));
                audit.setLastModifiedDate(formatDate(encounter.getMeta().getLastUpdated()));
            }
            dto.setAudit(audit);
        }

        return dto;
    }

    private List<EncounterDto> extractEncounters(Bundle bundle) {
        List<EncounterDto> encounters = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Encounter) {
                    encounters.add(toEncounterDto((Encounter) entry.getResource()));
                }
            }
        }
        return encounters;
    }

    private Encounter.EncounterStatus mapToFhirStatus(EncounterStatus status) {
        if (status == null) return Encounter.EncounterStatus.INPROGRESS;
        return switch (status) {
            case SIGNED -> Encounter.EncounterStatus.FINISHED;
            case UNSIGNED -> Encounter.EncounterStatus.INPROGRESS;
            case INCOMPLETE -> Encounter.EncounterStatus.TRIAGED;
        };
    }

    private EncounterStatus mapFromFhirStatus(Encounter.EncounterStatus status) {
        if (status == null) return EncounterStatus.UNSIGNED;
        return switch (status) {
            case FINISHED -> EncounterStatus.SIGNED;
            case INPROGRESS, ARRIVED, PLANNED -> EncounterStatus.UNSIGNED;
            default -> EncounterStatus.INCOMPLETE;
        };
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null) return null;
        try {
            LocalDate localDate = LocalDate.parse(dateStr, DAY);
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            return null;
        }
    }

    private String formatDate(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault())
                .toLocalDate().format(DAY);
    }

    // ✅ Get encounter by patient and encounter ID (alias for getByIdForPatient)
    public EncounterDto getEncounter(Long patientId, Long encounterId) {
        return getByIdForPatient(encounterId, patientId);
    }

    // ✅ Get all encounters (for browser service)
    public List<EncounterDto> getAllEncounters() {
        log.debug("Listing all FHIR Encounters");
        Bundle bundle = fhirClientService.search(Encounter.class, getPracticeId());
        return extractEncounters(bundle);
    }
}
