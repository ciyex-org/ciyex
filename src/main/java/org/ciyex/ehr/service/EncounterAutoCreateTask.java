package org.ciyex.ehr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.fhir.FhirClientService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Safety-net scheduled task that ensures encounters are created for
 * appointments with trigger statuses. Runs every 5 minutes.
 *
 * The primary encounter creation happens in real-time via FhirFacadeController
 * when a status is changed. This task catches any that were missed (e.g., due
 * to a transient error or server restart).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EncounterAutoCreateTask {

    private final AppointmentEncounterService appointmentEncounterService;
    private final FhirClientService fhirClientService;

    @Scheduled(fixedRateString = "${ciyex.encounter.safety-net.interval-ms:1800000}", initialDelay = 120_000) // default 30 min, start after 2 min
    public void ensureEncountersCreated() {
        Set<String> partitions = fhirClientService.getKnownPartitions();
        if (partitions.isEmpty()) {
            log.debug("No known FHIR partitions yet — skipping encounter safety-net check");
            return;
        }

        int totalCreated = 0;
        int totalChecked = 0;

        for (String orgAlias : partitions) {
            if (orgAlias == null || orgAlias.isBlank()) continue;

            try {
                List<Map<String, Object>> missing =
                        appointmentEncounterService.findAppointmentsMissingEncounters(orgAlias);
                totalChecked += missing.size();

                for (Map<String, Object> appt : missing) {
                    String encounterNote = (String) appt.get("_encounterNote");
                    String encId = appointmentEncounterService.createEncounterForAppointment(appt, encounterNote);
                    if (encId != null) {
                        totalCreated++;
                    }
                }
            } catch (Exception e) {
                log.error("Safety-net encounter check failed for org {}: {}", orgAlias, e.getMessage());
            }
        }

        if (totalCreated > 0 || totalChecked > 0) {
            log.info("Encounter safety-net: checked {} appointments, created {} encounters", totalChecked, totalCreated);
        }
    }
}
