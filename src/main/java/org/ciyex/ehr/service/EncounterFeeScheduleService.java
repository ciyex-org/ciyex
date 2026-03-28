package org.ciyex.ehr.service;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import org.ciyex.ehr.dto.FeeScheduleDto;
import org.ciyex.ehr.dto.FeeScheduleDto.FeeScheduleEntryDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only EncounterFeeSchedule Service.
 * Uses FHIR ChargeItemDefinition resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EncounterFeeScheduleService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // Extension URLs
    private static final String EXT_PATIENT = "http://ciyex.com/fhir/StructureDefinition/patient-reference";
    private static final String EXT_ENCOUNTER = "http://ciyex.com/fhir/StructureDefinition/encounter-reference";
    private static final String EXT_PAYER = "http://ciyex.com/fhir/StructureDefinition/payer";
    private static final String EXT_CURRENCY = "http://ciyex.com/fhir/StructureDefinition/currency";
    private static final String EXT_EFFECTIVE_FROM = "http://ciyex.com/fhir/StructureDefinition/effective-from";
    private static final String EXT_EFFECTIVE_TO = "http://ciyex.com/fhir/StructureDefinition/effective-to";
    private static final String EXT_NOTES = "http://ciyex.com/fhir/StructureDefinition/notes";
    private static final String EXT_ENTRIES = "http://ciyex.com/fhir/StructureDefinition/fee-entries";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public FeeScheduleDto create(Long patientId, Long encounterId, FeeScheduleDto dto) {
        log.debug("Creating FHIR ChargeItemDefinition (fee schedule) for patient: {} encounter: {}", patientId, encounterId);

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        ChargeItemDefinition cid = toFhirChargeItemDefinition(dto);
        var outcome = fhirClientService.create(cid, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setExternalId(fhirId);
        log.info("Created FHIR ChargeItemDefinition (fee schedule) with id: {}", fhirId);

        return dto;
    }

    // UPDATE
    public FeeScheduleDto update(Long patientId, Long encounterId, String fhirId, FeeScheduleDto dto) {
        log.debug("Updating FHIR ChargeItemDefinition (fee schedule): {}", fhirId);

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);
        dto.setExternalId(fhirId);

        ChargeItemDefinition cid = toFhirChargeItemDefinition(dto);
        cid.setId(fhirId);
        fhirClientService.update(cid, getPracticeId());

        return dto;
    }

    // DELETE
    public void delete(Long patientId, Long encounterId, String fhirId) {
        log.debug("Deleting FHIR ChargeItemDefinition (fee schedule): {}", fhirId);
        fhirClientService.delete(ChargeItemDefinition.class, fhirId, getPracticeId());
    }

    // GET ONE
    public FeeScheduleDto getOne(Long patientId, Long encounterId, String fhirId) {
        log.debug("Getting FHIR ChargeItemDefinition (fee schedule): {}", fhirId);
        ChargeItemDefinition cid = fhirClientService.read(ChargeItemDefinition.class, fhirId, getPracticeId());
        return fromFhirChargeItemDefinition(cid);
    }

    // LIST IN ENCOUNTER
    public List<FeeScheduleDto> listInEncounter(Long patientId, Long encounterId) {
        log.debug("Getting FHIR ChargeItemDefinitions for patient: {} encounter: {}", patientId, encounterId);

        Bundle bundle = fhirClientService.search(ChargeItemDefinition.class, getPracticeId());
        List<ChargeItemDefinition> defs = fhirClientService.extractResources(bundle, ChargeItemDefinition.class);

        return defs.stream()
                .map(this::fromFhirChargeItemDefinition)
                .filter(d -> patientId.equals(d.getPatientId()) && encounterId.equals(d.getEncounterId()))
                .collect(Collectors.toList());
    }

    // LIST BY PATIENT
    public List<FeeScheduleDto> listByPatient(Long patientId) {
        log.debug("Getting FHIR ChargeItemDefinitions for patient: {}", patientId);

        Bundle bundle = fhirClientService.search(ChargeItemDefinition.class, getPracticeId());
        List<ChargeItemDefinition> defs = fhirClientService.extractResources(bundle, ChargeItemDefinition.class);

        return defs.stream()
                .map(this::fromFhirChargeItemDefinition)
                .filter(d -> patientId.equals(d.getPatientId()))
                .collect(Collectors.toList());
    }

    // ADD ENTRY
    public FeeScheduleEntryDto addEntry(Long patientId, Long encounterId, String scheduleId, FeeScheduleEntryDto entry) {
        FeeScheduleDto schedule = getOne(patientId, encounterId, scheduleId);
        if (schedule.getEntries() == null) {
            schedule.setEntries(new ArrayList<>());
        }
        entry.setId((long) (schedule.getEntries().size() + 1));
        entry.setScheduleId(Long.parseLong(scheduleId.hashCode() + ""));
        schedule.getEntries().add(entry);
        update(patientId, encounterId, scheduleId, schedule);
        return entry;
    }

    // UPDATE ENTRY
    public FeeScheduleEntryDto updateEntry(Long patientId, Long encounterId, String scheduleId, Long entryId, FeeScheduleEntryDto entry) {
        FeeScheduleDto schedule = getOne(patientId, encounterId, scheduleId);
        if (schedule.getEntries() != null) {
            for (int i = 0; i < schedule.getEntries().size(); i++) {
                if (schedule.getEntries().get(i).getId().equals(entryId)) {
                    entry.setId(entryId);
                    schedule.getEntries().set(i, entry);
                    break;
                }
            }
        }
        update(patientId, encounterId, scheduleId, schedule);
        return entry;
    }

    // DELETE ENTRY
    public void deleteEntry(Long patientId, Long encounterId, String scheduleId, Long entryId) {
        FeeScheduleDto schedule = getOne(patientId, encounterId, scheduleId);
        if (schedule.getEntries() != null) {
            schedule.getEntries().removeIf(e -> e.getId().equals(entryId));
        }
        update(patientId, encounterId, scheduleId, schedule);
    }

    // LIST ENTRIES
    public List<FeeScheduleEntryDto> listEntries(Long patientId, Long encounterId, String scheduleId) {
        FeeScheduleDto schedule = getOne(patientId, encounterId, scheduleId);
        return schedule.getEntries() != null ? schedule.getEntries() : List.of();
    }

    // SEARCH ENTRIES
    public List<FeeScheduleEntryDto> searchEntries(Long patientId, Long encounterId, String scheduleId, String codeType, Boolean active, String q) {
        List<FeeScheduleEntryDto> entries = listEntries(patientId, encounterId, scheduleId);
        return entries.stream()
                .filter(e -> codeType == null || codeType.equals(e.getCodeType()))
                .filter(e -> active == null || active.equals(e.getActive()))
                .filter(e -> q == null || (e.getCode() != null && e.getCode().contains(q)) || (e.getDescription() != null && e.getDescription().contains(q)))
                .collect(Collectors.toList());
    }

    // -------- FHIR Mapping --------

    private ChargeItemDefinition toFhirChargeItemDefinition(FeeScheduleDto dto) {
        ChargeItemDefinition cid = new ChargeItemDefinition();
        cid.setStatus(Enumerations.PublicationStatus.ACTIVE);

        // Title/Name
        if (dto.getName() != null) {
            cid.setTitle(dto.getName());
        }

        // Extensions
        if (dto.getPatientId() != null) {
            cid.addExtension(new Extension(EXT_PATIENT, new Reference("Patient/" + dto.getPatientId())));
        }
        if (dto.getEncounterId() != null) {
            cid.addExtension(new Extension(EXT_ENCOUNTER, new Reference("Encounter/" + dto.getEncounterId())));
        }
        addStringExtension(cid, EXT_PAYER, dto.getPayer());
        addStringExtension(cid, EXT_CURRENCY, dto.getCurrency());
        addStringExtension(cid, EXT_EFFECTIVE_FROM, dto.getEffectiveFrom());
        addStringExtension(cid, EXT_EFFECTIVE_TO, dto.getEffectiveTo());
        addStringExtension(cid, EXT_NOTES, dto.getNotes());

        // Status
        if (dto.getStatus() != null) {
            cid.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/status", new StringType(dto.getStatus())));
        }

        // Entries as JSON extension
        if (dto.getEntries() != null && !dto.getEntries().isEmpty()) {
            StringBuilder entriesJson = new StringBuilder("[");
            for (int i = 0; i < dto.getEntries().size(); i++) {
                FeeScheduleEntryDto e = dto.getEntries().get(i);
                if (i > 0) entriesJson.append(",");
                entriesJson.append("{")
                        .append("\"id\":").append(e.getId() != null ? e.getId() : i + 1).append(",")
                        .append("\"codeType\":\"").append(e.getCodeType() != null ? e.getCodeType() : "").append("\",")
                        .append("\"code\":\"").append(e.getCode() != null ? e.getCode() : "").append("\",")
                        .append("\"modifier\":\"").append(e.getModifier() != null ? e.getModifier() : "").append("\",")
                        .append("\"description\":\"").append(e.getDescription() != null ? e.getDescription() : "").append("\",")
                        .append("\"unit\":\"").append(e.getUnit() != null ? e.getUnit() : "").append("\",")
                        .append("\"currency\":\"").append(e.getCurrency() != null ? e.getCurrency() : "").append("\",")
                        .append("\"amount\":").append(e.getAmount() != null ? e.getAmount() : 0).append(",")
                        .append("\"active\":").append(e.getActive() != null ? e.getActive() : true).append(",")
                        .append("\"notes\":\"").append(e.getNotes() != null ? e.getNotes() : "").append("\"")
                        .append("}");
            }
            entriesJson.append("]");
            cid.addExtension(new Extension(EXT_ENTRIES, new StringType(entriesJson.toString())));
        }

        return cid;
    }

    private FeeScheduleDto fromFhirChargeItemDefinition(ChargeItemDefinition cid) {
        FeeScheduleDto dto = new FeeScheduleDto();
        dto.setExternalId(cid.getIdElement().getIdPart());

        // Title/Name
        if (cid.hasTitle()) {
            dto.setName(cid.getTitle());
        }

        // Patient
        Extension patExt = cid.getExtensionByUrl(EXT_PATIENT);
        if (patExt != null && patExt.getValue() instanceof Reference) {
            String ref = ((Reference) patExt.getValue()).getReference();
            if (ref != null && ref.startsWith("Patient/")) {
                try { dto.setPatientId(Long.parseLong(ref.substring("Patient/".length()))); } catch (NumberFormatException ignored) {}
            }
        }

        // Encounter
        Extension encExt = cid.getExtensionByUrl(EXT_ENCOUNTER);
        if (encExt != null && encExt.getValue() instanceof Reference) {
            String ref = ((Reference) encExt.getValue()).getReference();
            if (ref != null && ref.startsWith("Encounter/")) {
                try { dto.setEncounterId(Long.parseLong(ref.substring("Encounter/".length()))); } catch (NumberFormatException ignored) {}
            }
        }

        dto.setPayer(getExtensionString(cid, EXT_PAYER));
        dto.setCurrency(getExtensionString(cid, EXT_CURRENCY));
        dto.setEffectiveFrom(getExtensionString(cid, EXT_EFFECTIVE_FROM));
        dto.setEffectiveTo(getExtensionString(cid, EXT_EFFECTIVE_TO));
        dto.setNotes(getExtensionString(cid, EXT_NOTES));
        dto.setStatus(getExtensionString(cid, "http://ciyex.com/fhir/StructureDefinition/status"));

        // Entries (simplified - would need JSON parsing in production)
        String entriesJson = getExtensionString(cid, EXT_ENTRIES);
        if (entriesJson != null) {
            dto.setEntries(parseEntriesJson(entriesJson));
        }

        return dto;
    }

    private List<FeeScheduleEntryDto> parseEntriesJson(String json) {
        // Simplified JSON parsing - in production use Jackson
        List<FeeScheduleEntryDto> entries = new ArrayList<>();
        // Basic parsing for demo - would use proper JSON library
        return entries;
    }

    // -------- Helpers --------

    private void addStringExtension(ChargeItemDefinition cid, String url, String value) {
        if (value != null) {
            cid.addExtension(new Extension(url, new StringType(value)));
        }
    }

    private String getExtensionString(ChargeItemDefinition cid, String url) {
        Extension ext = cid.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }
}
