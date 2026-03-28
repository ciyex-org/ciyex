package org.ciyex.ehr.service;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.fhir.FhirClientService;
import org.ciyex.ehr.tabconfig.service.TabFieldConfigService;
import org.ciyex.ehr.tabconfig.entity.TabFieldConfig;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Handles auto-creation of Encounter resources when appointment status changes
 * to a trigger status (e.g., arrived, checked-in, noshow).
 *
 * Status configuration is read from tab_field_config for the 'appointments' tab,
 * where each status option can specify triggersEncounter=true.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentEncounterService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final TabFieldConfigService tabFieldConfigService;
    private final ObjectMapper objectMapper;

    public record StatusOption(
            String value,
            String label,
            String color,
            boolean triggersEncounter,
            boolean terminal,
            int order,
            String nextStatus,
            String encounterNote
    ) {}

    /**
     * Read status options with metadata from tab_field_config for appointments.
     */
    private static final List<StatusOption> DEFAULT_STATUS_OPTIONS = List.of(
            new StatusOption("Scheduled",    "Scheduled",    "#3b82f6", false, false, 0, "Confirmed", null),
            new StatusOption("Confirmed",    "Confirmed",    "#6366f1", false, false, 1, "Checked-in", null),
            new StatusOption("Checked-in",   "Checked-in",   "#f59e0b", true,  false, 2, "Completed",  null),
            new StatusOption("Completed",    "Completed",    "#10b981", false, true,  3, null,         null),
            new StatusOption("Re-Scheduled", "Re-Scheduled", "#8b5cf6", false, false, 4, "Scheduled",  null),
            new StatusOption("No Show",      "No Show",      "#ef4444", false, true,  5, null,         null),
            new StatusOption("Cancelled",    "Cancelled",    "#6b7280", false, true,  6, null,         null)
    );

    /** Map custom status display names to valid FHIR Appointment.status codes. */
    private static final Map<String, String> CUSTOM_TO_FHIR = Map.of(
            "Scheduled",    "booked",
            "Confirmed",    "pending",
            "Checked-in",   "checked-in",
            "Completed",    "fulfilled",
            "Re-Scheduled", "proposed",
            "No Show",      "noshow",
            "Cancelled",    "cancelled"
    );

    /** Reverse map: FHIR code → custom status name. */
    private static final Map<String, String> FHIR_TO_CUSTOM;
    static {
        Map<String, String> m = new HashMap<>();
        CUSTOM_TO_FHIR.forEach((custom, fhir) -> m.put(fhir, custom));
        FHIR_TO_CUSTOM = Map.copyOf(m);
    }

    /**
     * Convert a custom status name to the corresponding FHIR Appointment.status code.
     * Returns the input unchanged if it is already a valid FHIR code.
     */
    public static String toFhirCode(String status) {
        if (status == null) return "booked";
        String fhir = CUSTOM_TO_FHIR.get(status);
        return fhir != null ? fhir : status;
    }

    /**
     * Convert a FHIR Appointment.status code to the custom display status name.
     * Returns the input unchanged if no mapping exists.
     */
    public static String fromFhirCode(String fhirCode) {
        if (fhirCode == null) return "Scheduled";
        String custom = FHIR_TO_CUSTOM.get(fhirCode);
        return custom != null ? custom : fhirCode;
    }

    public List<StatusOption> getStatusConfig() {
        return DEFAULT_STATUS_OPTIONS;
    }

    /**
     * Read room options from tab_field_config for appointments.
     */
    public List<String> getRoomOptions() {
        TabFieldConfig config = tabFieldConfigService.getEffectiveFieldConfig("appointments", "*", getOrgId());
        if (config == null) return List.of();

        try {
            JsonNode fieldConfig = objectMapper.readTree(config.getFieldConfig());
            JsonNode sections = fieldConfig.get("sections");
            if (sections == null || !sections.isArray()) return List.of();

            for (JsonNode section : sections) {
                JsonNode fields = section.get("fields");
                if (fields == null || !fields.isArray()) continue;

                for (JsonNode field : fields) {
                    if (!"room".equals(field.path("key").asText())) continue;

                    JsonNode options = field.get("options");
                    if (options == null || !options.isArray()) return List.of();

                    List<String> result = new ArrayList<>();
                    for (JsonNode opt : options) {
                        if (opt.isTextual()) {
                            result.add(opt.asText());
                        } else if (opt.isObject()) {
                            result.add(opt.path("value").asText(opt.path("label").asText()));
                        }
                    }
                    return result;
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse room options from tab_field_config", e);
        }

        // Fallback: return default room options when none are configured in tab_field_config
        return List.of("Exam 1", "Exam 2", "Exam 3", "Exam 4", "Lab", "Procedure Room", "Triage");
    }

    /**
     * Find the StatusOption for a given status value.
     */
    public Optional<StatusOption> findStatusOption(String statusValue) {
        return getStatusConfig().stream()
                .filter(s -> s.value().equals(statusValue))
                .findFirst();
    }

    /**
     * Check if an encounter already exists for an appointment.
     */
    public boolean encounterExistsForAppointment(String appointmentFhirId) {
        return findEncounterForAppointment(appointmentFhirId) != null;
    }

    /**
     * Find existing encounter linked to an appointment. Returns encounter FHIR ID or null.
     */
    public String findEncounterForAppointment(String appointmentFhirId) {
        String orgAlias = practiceContextService.getPracticeId();
        try {
            Bundle bundle = fhirClientService.getClient(orgAlias).search()
                    .forResource(Encounter.class)
                    .where(new ReferenceClientParam("appointment")
                            .hasId("Appointment/" + appointmentFhirId))
                    .returnBundle(Bundle.class)
                    .execute();

            if (bundle.hasEntry() && !bundle.getEntry().isEmpty()) {
                return bundle.getEntryFirstRep().getResource().getIdElement().getIdPart();
            }
        } catch (Exception e) {
            log.warn("Failed to find encounter for appointment {}: {}", appointmentFhirId, e.getMessage());
        }
        return null;
    }

    /**
     * Auto-create an Encounter from appointment data when status triggers encounter creation.
     * Returns the encounter FHIR ID, or null if creation failed.
     * If encounter already exists, returns the existing encounter ID.
     */
    public String createEncounterForAppointment(Map<String, Object> appointmentData, String encounterNote) {
        String appointmentId = String.valueOf(appointmentData.get("id"));

        // Duplicate check
        String existingId = findEncounterForAppointment(appointmentId);
        if (existingId != null) {
            log.info("Encounter already exists ({}) for appointment {}", existingId, appointmentId);
            return existingId;
        }

        String orgAlias = practiceContextService.getPracticeId();

        try {
            Encounter encounter = new Encounter();

            // Status: in-progress (unsigned)
            encounter.setStatus(Encounter.EncounterStatus.INPROGRESS);

            // Class: ambulatory
            encounter.setClass_(new Coding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                    .setCode("AMB")
                    .setDisplay("ambulatory"));

            // Subject: Patient reference from appointment
            Object patientRef = appointmentData.get("patient");
            if (patientRef instanceof String ref && ref.startsWith("Patient/")) {
                encounter.setSubject(new Reference(ref));
            }

            // Participant: Provider reference from appointment
            Object providerRef = appointmentData.get("provider");
            if (providerRef instanceof String ref && ref.startsWith("Practitioner/")) {
                encounter.addParticipant()
                        .setIndividual(new Reference(ref));
            }

            // Link back to the appointment
            encounter.addAppointment(new Reference("Appointment/" + appointmentId));

            // Period start from appointment start time
            Object start = appointmentData.get("start");
            if (start instanceof String s && !s.isBlank()) {
                try {
                    Date startDate = Date.from(Instant.parse(s));
                    encounter.setPeriod(new Period().setStart(startDate));
                } catch (Exception e) {
                    encounter.setPeriod(new Period().setStart(new Date()));
                }
            } else {
                encounter.setPeriod(new Period().setStart(new Date()));
            }

            // Type: visit type from appointment
            Object visitType = appointmentData.get("appointmentType");
            if (visitType instanceof String vt && !vt.isBlank()) {
                encounter.addType().setText(vt);
            }

            // Reason: appointment reason + encounterNote (e.g., "No-show")
            Object reason = appointmentData.get("reason");
            String reasonText = "";
            if (reason instanceof String r && !r.isBlank()) {
                reasonText = r;
            }
            if (encounterNote != null && !encounterNote.isBlank()) {
                reasonText = reasonText.isEmpty() ? encounterNote : reasonText + " | " + encounterNote;
            }
            if (!reasonText.isEmpty()) {
                encounter.addReasonCode().setText(reasonText);
            }

            // Create in FHIR
            var outcome = fhirClientService.create(encounter, orgAlias);
            String fhirId = outcome.getId().getIdPart();

            log.info("Auto-created encounter {} for appointment {} (note: {})", fhirId, appointmentId, encounterNote);
            return fhirId;

        } catch (Exception e) {
            log.error("Failed to auto-create encounter for appointment {}: {}", appointmentId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Handle appointment status change — check if the new status triggers encounter creation.
     * Returns the encounter FHIR ID if created/found, or null if no encounter action needed.
     */
    public String handleStatusChange(String appointmentId, String newStatus, Map<String, Object> appointmentData) {
        Optional<StatusOption> statusOpt = findStatusOption(newStatus);
        if (statusOpt.isEmpty()) {
            log.debug("Status '{}' not found in config — no encounter action", newStatus);
            return null;
        }

        StatusOption status = statusOpt.get();
        if (!status.triggersEncounter()) {
            return null;
        }

        log.info("Status '{}' triggers encounter creation for appointment {}", newStatus, appointmentId);
        return createEncounterForAppointment(appointmentData, status.encounterNote());
    }

    /**
     * Extract patient FHIR ID from appointment data (handles both reference and raw ID formats).
     */
    public Long getPatientIdFromAppointment(Map<String, Object> appointmentData) {
        // Try FHIR reference format first
        Object patient = appointmentData.get("patient");
        if (patient instanceof String ref && ref.startsWith("Patient/")) {
            try {
                return Long.parseLong(ref.substring(8));
            } catch (NumberFormatException e) {
                // non-numeric FHIR ID
            }
        }

        // Try raw patientId
        Object patientId = appointmentData.get("patientId");
        if (patientId instanceof Number n) return n.longValue();
        if (patientId instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Batch-find encounters linked to the given appointment IDs.
     * Returns a map of appointmentId → [encounterId, patientId].
     */
    public Map<String, String[]> findEncounterMapForAppointments(List<String> appointmentIds) {
        Map<String, String[]> result = new HashMap<>();
        if (appointmentIds == null || appointmentIds.isEmpty()) return result;

        log.debug("Looking up encounters for {} appointments", appointmentIds.size());

        // Use individual lookups — reliable across all FHIR server implementations
        for (String apptId : appointmentIds) {
            try {
                String encId = findEncounterForAppointment(apptId);
                if (encId != null) {
                    // Get patient ID from the encounter
                    String orgAlias = practiceContextService.getPracticeId();
                    String patientId = null;
                    try {
                        Encounter enc = fhirClientService.getClient(orgAlias)
                                .read().resource(Encounter.class).withId(encId).execute();
                        if (enc.hasSubject() && enc.getSubject().hasReference()) {
                            String ref = enc.getSubject().getReference();
                            if (ref.startsWith("Patient/")) {
                                patientId = ref.substring(8);
                            }
                        }
                    } catch (Exception ignored) {
                        // Patient ID is optional enrichment
                    }
                    result.put(apptId, new String[]{encId, patientId});
                }
            } catch (Exception e) {
                log.debug("Failed to find encounter for appointment {}: {}", apptId, e.getMessage());
            }
        }

        log.debug("Found {} encounters for {} appointments", result.size(), appointmentIds.size());
        return result;
    }

    /**
     * Get today's appointments that have a trigger status but no encounter.
     * Used by the scheduled safety-net task.
     */
    public List<Map<String, Object>> findAppointmentsMissingEncounters(String orgAlias) {
        List<StatusOption> triggerStatuses = getStatusConfig().stream()
                .filter(StatusOption::triggersEncounter)
                .toList();

        if (triggerStatuses.isEmpty()) return List.of();

        Set<String> triggerValues = new HashSet<>();
        for (StatusOption s : triggerStatuses) {
            triggerValues.add(s.value());
        }

        // Search today's appointments
        try {
            Bundle bundle = fhirClientService.getClient(orgAlias).search()
                    .forResource(Appointment.class)
                    .where(Appointment.DATE.exactly().day(new Date()))
                    .returnBundle(Bundle.class)
                    .execute();

            List<Map<String, Object>> missing = new ArrayList<>();
            if (bundle.hasEntry()) {
                for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                    if (entry.getResource() instanceof Appointment appt) {
                        String status = appt.getStatus() != null ? appt.getStatus().toCode() : null;
                        if (status != null && triggerValues.contains(status)) {
                            String apptId = appt.getIdElement().getIdPart();
                            if (!encounterExistsForAppointment(apptId)) {
                                Map<String, Object> data = new LinkedHashMap<>();
                                data.put("id", apptId);
                                data.put("status", status);
                                // Extract patient reference
                                for (Appointment.AppointmentParticipantComponent p : appt.getParticipant()) {
                                    if (p.hasActor() && p.getActor().hasReference()) {
                                        String ref = p.getActor().getReference();
                                        if (ref.startsWith("Patient/")) data.put("patient", ref);
                                        if (ref.startsWith("Practitioner/")) data.put("provider", ref);
                                    }
                                }
                                if (appt.hasStart()) {
                                    data.put("start", appt.getStart().toInstant().toString());
                                }
                                if (appt.hasAppointmentType() && appt.getAppointmentType().hasText()) {
                                    data.put("appointmentType", appt.getAppointmentType().getText());
                                }
                                // Find the encounterNote for this status
                                triggerStatuses.stream()
                                        .filter(s -> s.value().equals(status))
                                        .findFirst()
                                        .ifPresent(s -> data.put("_encounterNote", s.encounterNote()));
                                missing.add(data);
                            }
                        }
                    }
                }
            }
            return missing;
        } catch (Exception e) {
            log.error("Failed to find appointments missing encounters for org {}: {}", orgAlias, e.getMessage());
            return List.of();
        }
    }

    private String getOrgId() {
        try {
            String id = practiceContextService.getPracticeId();
            return id != null ? id : "*";
        } catch (Exception e) {
            return "*";
        }
    }
}
