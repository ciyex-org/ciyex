package org.ciyex.ehr.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.ScheduleDto;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.fhir.GenericFhirResourceService;
import org.ciyex.ehr.notification.service.AppointmentNotificationService;
import org.ciyex.ehr.service.AppointmentEncounterService;
import org.ciyex.ehr.service.KeycloakAdminService;
import org.ciyex.ehr.service.KeycloakUserService;
import org.ciyex.ehr.service.ScheduleService;
import org.ciyex.ehr.service.SlotService;
import org.ciyex.ehr.usermgmt.service.EmailService;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Facade controller that maps legacy/frontend API paths to the generic FHIR resource handler.
 * ALL FHIR resource endpoints flow through here, delegating to GenericFhirResourceService.
 * This is the ONLY bridge layer between frontend API paths and the generic FHIR service.
 */
@PreAuthorize("isAuthenticated()")
@RestController
@RequiredArgsConstructor
@Slf4j
public class FhirFacadeController {

    private final GenericFhirResourceService fhirService;
    private final AppointmentEncounterService appointmentEncounterService;
    private final AppointmentNotificationService appointmentNotificationService;
    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakUserService keycloakUserService;
    private final ScheduleService scheduleService;
    private final SlotService slotService;
    private final EmailService emailService;

    // ==================== LOCATIONS (tab: facilities) ====================

    @GetMapping("/api/locations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listLocations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        try {
            Map<String, Object> data = fhirService.listAll("facilities", page, size);
            return ResponseEntity.ok(ApiResponse.ok("Locations retrieved", data));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to list locations", e);
            return ResponseEntity.ok(ApiResponse.ok("Locations retrieved", emptyPage(page, size)));
        }
    }

    /** /api/facilities — alias for /api/locations, supports ?search= for lookup fields */
    @GetMapping("/api/facilities")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listFacilities(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Map<String, Object> data = fhirService.listAll("facilities", page, size);
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            if (search != null && !search.isBlank()) {
                String q = search.toLowerCase();
                content = content.stream()
                        .filter(loc -> {
                            Object nameObj = loc.get("name");
                            return nameObj != null && String.valueOf(nameObj).toLowerCase().contains(q);
                        })
                        .toList();
            }
            return ResponseEntity.ok(ApiResponse.ok("Facilities retrieved", content));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to list facilities", e);
            return ResponseEntity.ok(ApiResponse.ok("Facilities retrieved", List.of()));
        }
    }

    // ==================== PRACTICES (tab: practice) ====================

    @GetMapping("/api/practices")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listPractices() {
        try {
            Map<String, Object> data = fhirService.listAll("practice", 0, 100);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            List<Map<String, Object>> nested = content.stream().map(FhirFacadeController::flatToNested).toList();
            return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                    .success(true).message("Practices retrieved successfully from FHIR").data(nested).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to list practices", e);
            return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                    .success(false).message("Failed to retrieve practices").data(null).build());
        }
    }

    @PostMapping("/api/practices")

    public ResponseEntity<ApiResponse<Map<String, Object>>> createPractice(@RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> flat = nestedToFlat(body);
            Map<String, Object> created = fhirService.create("practice", null, flat);
            return ResponseEntity.ok(ApiResponse.ok("Practice saved successfully", flatToNested(created)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create practice", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to save practice"));
        }
    }

    @GetMapping("/api/practices/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPractice(@PathVariable String id) {
        try {
            Map<String, Object> data = fhirService.get("practice", null, id);
            if (data == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Practice not found"));
            }
            return ResponseEntity.ok(ApiResponse.ok("Practice retrieved successfully", flatToNested(data)));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to retrieve practice {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve practice: " + e.getMessage()));
        }
    }

    @PutMapping("/api/practices/{id}")

    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePractice(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            // Get existing, merge with incoming
            Map<String, Object> existing = fhirService.get("practice", null, id);
            if (existing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Practice not found"));
            }
            Map<String, Object> flat = nestedToFlat(body);
            // Merge: incoming overrides existing
            for (Map.Entry<String, Object> entry : flat.entrySet()) {
                if (entry.getValue() != null) {
                    existing.put(entry.getKey(), entry.getValue());
                }
            }
            // Handle tokenExpiryMinutes → Keycloak side effect
            Object tokenExpiry = flat.get("practiceSettings.tokenExpiryMinutes");
            if (tokenExpiry != null) {
                try {
                    int minutes = tokenExpiry instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(tokenExpiry));
                    keycloakAdminService.updateClientTokenLifespan(minutes);
                } catch (Exception ex) {
                    log.warn("Failed to update Keycloak token lifespan: {}", ex.getMessage());
                }
            }
            Map<String, Object> updated = fhirService.update("practice", null, id, existing);
            return ResponseEntity.ok(ApiResponse.ok("Practice updated successfully", flatToNested(updated)));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update practice {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update practice"));
        }
    }

    @DeleteMapping("/api/practices/{id}")

    public ResponseEntity<ApiResponse<Void>> deletePractice(@PathVariable String id) {
        try {
            fhirService.delete("practice", id);
            return ResponseEntity.ok(ApiResponse.ok("Practice deleted successfully", null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete practice {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Practice not found"));
        }
    }

    @DeleteMapping("/api/practices/all")

    public ResponseEntity<ApiResponse<Void>> deleteAllPractices() {
        try {
            // Delete all practice resources
            Map<String, Object> data = fhirService.listAll("practice", 0, 100);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            for (Map<String, Object> p : content) {
                String fhirId = String.valueOf(p.get("fhirId"));
                if (fhirId != null && !"null".equals(fhirId)) {
                    try { fhirService.delete("practice", fhirId); } catch (Exception ignored) {}
                }
            }
            return ResponseEntity.ok(ApiResponse.ok("All practices deleted successfully", null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete all practices", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete all practices"));
        }
    }

    @GetMapping("/api/practices/count")
    public ResponseEntity<ApiResponse<Long>> getPracticeCount() {
        try {
            long count = fhirService.count("practice");
            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .success(true).message("Practice count retrieved successfully").data(count).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to count practices", e);
            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .success(false).message("Failed to retrieve practice count").data(null).build());
        }
    }

    @GetMapping("/api/practices/search")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchPracticesByName(@RequestParam String name) {
        try {
            Map<String, Object> data = fhirService.searchByName("practice", name, 0, 100);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            List<Map<String, Object>> nested = content.stream().map(FhirFacadeController::flatToNested).toList();
            return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                    .success(true).message("Practices retrieved successfully").data(nested).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to search practices", e);
            return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                    .success(false).message("Failed to search practices").data(null).build());
        }
    }

    @GetMapping("/api/practices/{identifier}/practice-settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPracticeSettings(@PathVariable String identifier) {
        try {
            Map<String, Object> practice = findPractice(identifier);
            if (practice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Practice not found"));
            }
            Map<String, Object> nested = flatToNested(practice);
            @SuppressWarnings("unchecked")
            Map<String, Object> settings = (Map<String, Object>) nested.getOrDefault("practiceSettings", new LinkedHashMap<>());
            return ResponseEntity.ok(ApiResponse.ok("Practice settings retrieved successfully", settings));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get practice settings for {}", identifier, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve practice settings"));
        }
    }

    @PutMapping("/api/practices/{id}/practice-settings")

    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePracticeSettings(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> existing = fhirService.get("practice", null, id);
            if (existing == null) {
                // Try finding the first practice
                existing = findFirstPractice();
                if (existing == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Practice not found"));
                }
                id = String.valueOf(existing.get("fhirId"));
            }
            // Merge practice settings
            for (Map.Entry<String, Object> entry : body.entrySet()) {
                if (entry.getValue() != null) {
                    existing.put("practiceSettings." + entry.getKey(), entry.getValue());
                }
            }
            // Keycloak side effect
            if (body.containsKey("tokenExpiryMinutes") && body.get("tokenExpiryMinutes") != null) {
                try {
                    int minutes = body.get("tokenExpiryMinutes") instanceof Number n ? n.intValue()
                            : Integer.parseInt(String.valueOf(body.get("tokenExpiryMinutes")));
                    keycloakAdminService.updateClientTokenLifespan(minutes);
                } catch (Exception ex) {
                    log.warn("Failed to update Keycloak token lifespan: {}", ex.getMessage());
                }
            }
            Map<String, Object> updated = fhirService.update("practice", null, id, existing);
            return ResponseEntity.ok(ApiResponse.ok("Practice settings updated successfully", flatToNested(updated)));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update practice settings for {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update practice settings"));
        }
    }

    @GetMapping("/api/practices/{identifier}/regional-settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRegionalSettings(@PathVariable String identifier) {
        try {
            Map<String, Object> practice = findPractice(identifier);
            if (practice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Practice not found"));
            }
            Map<String, Object> nested = flatToNested(practice);
            @SuppressWarnings("unchecked")
            Map<String, Object> settings = (Map<String, Object>) nested.getOrDefault("regionalSettings", new LinkedHashMap<>());
            return ResponseEntity.ok(ApiResponse.ok("Regional settings retrieved successfully", settings));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get regional settings for {}", identifier, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve regional settings"));
        }
    }

    @PutMapping("/api/practices/{id}/regional-settings")

    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRegionalSettings(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> existing = fhirService.get("practice", null, id);
            if (existing == null) {
                existing = findFirstPractice();
                if (existing == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Practice not found"));
                }
                id = String.valueOf(existing.get("fhirId"));
            }
            for (Map.Entry<String, Object> entry : body.entrySet()) {
                if (entry.getValue() != null) {
                    existing.put("regionalSettings." + entry.getKey(), entry.getValue());
                }
            }
            Map<String, Object> updated = fhirService.update("practice", null, id, existing);
            return ResponseEntity.ok(ApiResponse.ok("Regional settings updated successfully", flatToNested(updated)));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update regional settings for {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update regional settings"));
        }
    }

    // ==================== PROVIDERS (tab: providers) ====================

    @GetMapping("/api/providers")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listProviders(
            @RequestParam(value = "search", required = false) String search) {
        try {
            Map<String, Object> data;
            if (search != null && !search.isBlank()) {
                data = fhirService.searchByName("providers", search, 0, 200);
            } else {
                data = fhirService.listAll("providers", 0, 200);
            }
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            List<Map<String, Object>> nested = content.stream()
                    .map(FhirFacadeController::flatToNested)
                    .map(this::enrichProviderFields)
                    .toList();

            // Client-side search filter (for NPI matching)
            if (search != null && !search.isBlank()) {
                String term = search.toLowerCase();
                nested = nested.stream().filter(p -> {
                    String name = String.valueOf(p.getOrDefault("name", ""));
                    String npi = String.valueOf(p.getOrDefault("npi", ""));
                    return name.toLowerCase().contains(term) || npi.contains(term);
                }).toList();
            }

            return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                    .success(true).message("Providers retrieved successfully from FHIR").data(nested).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to retrieve providers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<Map<String, Object>>>builder()
                            .success(false).message("Failed to retrieve providers").data(null).build());
        }
    }

    @PostMapping("/api/providers")

    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createProvider(@RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> flat = nestedToFlat(body);
            Map<String, Object> created = fhirService.create("providers", null, flat);
            String fhirId = String.valueOf(created.get("fhirId"));

            // Auto-create default Mon-Fri 8am-5pm schedule
            try {
                ScheduleDto defaultSchedule = new ScheduleDto();
                defaultSchedule.setProviderId(toLong(fhirId));
                defaultSchedule.setStatus("active");
                defaultSchedule.setTimezone("America/New_York");
                defaultSchedule.setServiceType("Office Visit");
                ScheduleDto.Recurrence recurrence = new ScheduleDto.Recurrence();
                recurrence.setFrequency("WEEKLY");
                recurrence.setInterval(1);
                recurrence.setByWeekday(List.of("MO", "TU", "WE", "TH", "FR"));
                recurrence.setStartDate(LocalDate.now().toString());
                recurrence.setStartTime("08:00");
                recurrence.setEndTime("17:00");
                defaultSchedule.setRecurrence(recurrence);
                scheduleService.create(defaultSchedule);
                log.info("Created default schedule for provider {}", fhirId);
            } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
                log.warn("Failed to create default schedule for provider {}: {}", fhirId, e.getMessage());
            }

            Map<String, Object> result = flatToNested(created);
            enrichProviderFields(result);

            // Auto-create Keycloak user if systemAccess.email is provided
            try {
                Map<String, Object> systemAccess = (Map<String, Object>) body.get("systemAccess");
                if (systemAccess != null) {
                    String loginEmail = (String) systemAccess.get("email");
                    if (loginEmail != null && !loginEmail.isBlank()) {
                        String orgAlias = RequestContext.get().getOrgName();
                        Map<String, Object> identification = (Map<String, Object>) body.get("identification");
                        String firstName = identification != null ? String.valueOf(identification.getOrDefault("firstName", "")) : "";
                        String lastName = identification != null ? String.valueOf(identification.getOrDefault("lastName", "")) : "";
                        String role = String.valueOf(systemAccess.getOrDefault("role", "PROVIDER"));
                        String tempPassword = keycloakUserService.generateTempPassword();

                        String userId = keycloakUserService.createUser(
                                loginEmail.trim(), firstName.trim(), lastName.trim(), tempPassword,
                                Map.of("practitioner_fhir_id", fhirId));
                        if (userId != null) {
                            keycloakUserService.addUserToOrganization(userId, orgAlias);
                            keycloakUserService.assignRolesToUser(userId, List.of(role));
                            log.info("Created Keycloak account for provider {} with role {}", fhirId, role);

                            // Add account info to response
                            Map<String, Object> sa = (Map<String, Object>) result.getOrDefault("systemAccess", new LinkedHashMap<>());
                            sa.put("keycloakUserId", userId);
                            sa.put("email", loginEmail.trim());
                            sa.put("hasAccount", true);
                            result.put("systemAccess", sa);
                            result.put("temporaryPassword", tempPassword);
                        }
                    }
                }
            } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
                log.warn("Failed to create Keycloak user for provider {}: {}", fhirId, e.getMessage());
            }

            return ResponseEntity.ok(ApiResponse.ok("Provider created successfully", result));
        } catch (IllegalArgumentException e) {
            log.error("Failed to create Provider: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to create provider: " + e.getMessage()).data(null).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create Provider", e);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to create provider: " + e.getMessage()).data(null).build());
        }
    }

    @GetMapping("/api/providers/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProvider(@PathVariable String id) {
        try {
            Map<String, Object> data = fhirService.get("providers", null, id);
            if (data == null) {
                return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                        .success(false).message("Failed to retrieve provider: Provider not found with FHIR ID: " + id).data(null).build());
            }
            Map<String, Object> result = flatToNested(data);
            enrichProviderFields(result);
            return ResponseEntity.ok(ApiResponse.ok("Provider retrieved successfully", result));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to retrieve Provider with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to retrieve provider: " + e.getMessage()).data(null).build());
        }
    }

    @PutMapping("/api/providers/{id}")

    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProvider(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> flat = nestedToFlat(body);
            Map<String, Object> updated = fhirService.update("providers", null, id, flat);
            Map<String, Object> result = flatToNested(updated);
            enrichProviderFields(result);
            return ResponseEntity.ok(ApiResponse.ok("Provider updated successfully", result));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update Provider with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to update provider: " + e.getMessage()).data(null).build());
        }
    }

    @DeleteMapping("/api/providers/{id}")

    public ResponseEntity<ApiResponse<Void>> deleteProvider(@PathVariable String id) {
        try {
            fhirService.delete("providers", id);
            return ResponseEntity.ok(ApiResponse.ok("Provider deleted successfully", null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete Provider with id {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>builder().success(false).message("Provider not found").data(null).build());
        }
    }

    @GetMapping("/api/providers/count")
    public ResponseEntity<ApiResponse<Long>> getProviderCount() {
        try {
            long count = fhirService.count("providers");
            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .success(true).message("Provider count retrieved successfully").data(count).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to count providers", e);
            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .success(false).message("Failed to retrieve provider count").data(null).build());
        }
    }

    @PutMapping("/api/providers/{id}/status")

    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProviderStatus(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            // Read raw Practitioner, set active flag, update
            org.hl7.fhir.r4.model.Resource raw = fhirService.readRawResource("Practitioner", id);
            if (raw instanceof org.hl7.fhir.r4.model.Practitioner practitioner) {
                String statusStr = String.valueOf(body.get("status"));
                practitioner.setActive("ACTIVE".equalsIgnoreCase(statusStr));
                fhirService.updateRawResource(practitioner);
            }
            Map<String, Object> data = fhirService.get("providers", null, id);
            Map<String, Object> result = flatToNested(data != null ? data : Map.of());
            enrichProviderFields(result);
            return ResponseEntity.ok(ApiResponse.ok("Provider status updated successfully", result));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update Provider status with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to update provider status: " + e.getMessage()).data(null).build());
        }
    }

    @GetMapping("/api/providers/{id}/account-status")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProviderAccountStatus(@PathVariable String id) {
        try {
            Map<String, Object> kcUser = keycloakUserService.findUserByAttribute("practitioner_fhir_id", id);
            Map<String, Object> data = new LinkedHashMap<>();
            if (kcUser != null) {
                data.put("hasAccount", true);
                data.put("keycloakUserId", kcUser.get("id"));
                data.put("email", kcUser.get("email"));
                data.put("accountEnabled", kcUser.getOrDefault("enabled", false));
            } else {
                data.put("hasAccount", false);
            }
            return ResponseEntity.ok(ApiResponse.ok("Account status retrieved", data));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get account status for provider {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to get account status").data(null).build());
        }
    }

    @PostMapping("/api/providers/{id}/reset-password")

    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetProviderPassword(@PathVariable String id) {
        try {
            Map<String, Object> kcUser = keycloakUserService.findUserByAttribute("practitioner_fhir_id", id);
            if (kcUser == null) {
                return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                        .success(false).message("No Keycloak account linked to this provider").data(null).build());
            }
            String userId = (String) kcUser.get("id");
            String tempPassword = keycloakUserService.resetPassword(userId);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("userId", userId);
            data.put("username", kcUser.get("email"));
            data.put("temporaryPassword", tempPassword);
            data.put("resetDate", LocalDate.now().toString());
            return ResponseEntity.ok(ApiResponse.ok("Password reset successfully", data));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to reset password for provider {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to reset password: " + e.getMessage()).data(null).build());
        }
    }

    @PostMapping("/api/providers/{id}/send-reset-email")

    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Void>> sendProviderResetEmail(@PathVariable String id) {
        try {
            Map<String, Object> kcUser = keycloakUserService.findUserByAttribute("practitioner_fhir_id", id);
            if (kcUser == null) {
                return ResponseEntity.ok(ApiResponse.<Void>builder()
                        .success(false).message("No Keycloak account linked to this provider").data(null).build());
            }
            String kcUserId = (String) kcUser.get("id");

            // Try Keycloak built-in email first
            try {
                keycloakUserService.sendPasswordResetEmail(kcUserId);
                return ResponseEntity.ok(ApiResponse.ok("Password reset email sent", null));
            } catch (Exception keycloakEx) {
                log.warn("Keycloak email failed for provider {}, falling back to app EmailService: {}", id, keycloakEx.getMessage());
            }

            // Fallback: reset password and send via app's EmailService
            String email = (String) kcUser.get("email");
            if (email == null || email.isBlank()) {
                return ResponseEntity.ok(ApiResponse.<Void>builder()
                        .success(false).message("Provider has no email address configured").data(null).build());
            }
            String tempPassword = keycloakUserService.resetPassword(kcUserId);
            String username = (String) kcUser.get("username");
            String firstName = (String) kcUser.get("firstName");
            String orgAlias = RequestContext.get().getOrgName();
            String htmlBody = buildResetEmailHtml(firstName, username, tempPassword);
            emailService.sendEmail(orgAlias, email, "Password Reset - Your Temporary Credentials", htmlBody);
            return ResponseEntity.ok(ApiResponse.ok("Password reset email sent", null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to send reset email for provider {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false).message("Failed to send reset email: " + e.getMessage()).data(null).build());
        }
    }

    @PutMapping("/api/providers/{id}/toggle-account")

    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleProviderAccount(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> kcUser = keycloakUserService.findUserByAttribute("practitioner_fhir_id", id);
            if (kcUser == null) {
                return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                        .success(false).message("No Keycloak account linked to this provider").data(null).build());
            }
            String userId = (String) kcUser.get("id");
            boolean enable = Boolean.TRUE.equals(body.get("enabled"));
            if (enable) {
                keycloakUserService.enableUser(userId);
            } else {
                keycloakUserService.disableUser(userId);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("userId", userId);
            data.put("enabled", enable);
            return ResponseEntity.ok(ApiResponse.ok(enable ? "Account enabled" : "Account blocked", data));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to toggle account for provider {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to toggle account: " + e.getMessage()).data(null).build());
        }
    }

    // ==================== PROVIDER AVAILABILITY ====================

    @GetMapping("/api/providers/{providerId}/availability")
    public ResponseEntity<ApiResponse<List<ScheduleDto>>> getProviderAvailability(
            @PathVariable Long providerId) {
        try {
            List<ScheduleDto> schedules = scheduleService.getByProviderId(providerId);
            return ResponseEntity.ok(ApiResponse.<List<ScheduleDto>>builder()
                    .success(true).message("Provider availability retrieved").data(schedules).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get availability for provider {}: {}", providerId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<List<ScheduleDto>>builder()
                    .success(false).message("Failed to retrieve availability: " + e.getMessage()).build());
        }
    }

    @PutMapping("/api/providers/{providerId}/availability")

    public ResponseEntity<ApiResponse<List<ScheduleDto>>> saveProviderAvailability(
            @PathVariable Long providerId, @RequestBody List<ScheduleDto> blocks) {
        try {
            List<ScheduleDto> existing = scheduleService.getByProviderId(providerId);
            Set<String> existingIds = existing.stream()
                    .map(ScheduleDto::getFhirId).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<String> submittedIds = blocks.stream()
                    .map(ScheduleDto::getFhirId).filter(Objects::nonNull).collect(Collectors.toSet());

            // Delete removed schedules
            for (ScheduleDto ex : existing) {
                if (ex.getFhirId() != null && !submittedIds.contains(ex.getFhirId())) {
                    scheduleService.delete(ex.getFhirId());
                }
            }

            // Create or update each block
            List<ScheduleDto> saved = new ArrayList<>();
            for (ScheduleDto block : blocks) {
                block.setProviderId(providerId);
                if (block.getStatus() == null) block.setStatus("active");
                if (block.getFhirId() != null && existingIds.contains(block.getFhirId())) {
                    saved.add(scheduleService.update(block.getFhirId(), block));
                } else {
                    saved.add(scheduleService.create(block));
                }
            }

            return ResponseEntity.ok(ApiResponse.<List<ScheduleDto>>builder()
                    .success(true).message("Provider availability saved (" + saved.size() + " blocks)").data(saved).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to save availability for provider {}: {}", providerId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<List<ScheduleDto>>builder()
                    .success(false).message("Failed to save availability: " + e.getMessage()).build());
        }
    }

    private String buildResetEmailHtml(String firstName, String username, String tempPassword) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;'>"
                + "<h2 style='color:#1e40af;'>Password Reset</h2>"
                + "<p>Hello " + (firstName != null ? firstName : "") + ",</p>"
                + "<p>Your password has been reset. Please use the following temporary credentials to log in:</p>"
                + "<div style='background:#f1f5f9;padding:16px;border-radius:8px;margin:16px 0;'>"
                + "<p style='margin:4px 0;'><strong>Username:</strong> " + username + "</p>"
                + "<p style='margin:4px 0;'><strong>Temporary Password:</strong> " + tempPassword + "</p>"
                + "</div>"
                + "<p>You will be required to change your password upon first login.</p>"
                + "<p style='color:#64748b;font-size:12px;'>This is an automated message. Please do not reply.</p>"
                + "</div>";
    }

    // ==================== PATIENTS (tab: demographics) ====================

    @GetMapping("/api/patients")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listPatients(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", required = false, defaultValue = "id") String sort,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "gender", required = false) String gender) {
        try {
            Map<String, Object> data;
            if (search != null && !search.isBlank()) {
                data = fhirService.searchByName("demographics", search, 0, 500);
            } else {
                data = fhirService.listAll("demographics", 0, 500);
            }
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            List<Map<String, Object>> nested = content.stream()
                    .map(FhirFacadeController::flatToNested)
                    .map(FhirFacadeController::enrichPatientFields)
                    .collect(Collectors.toList());

            // Filter by status
            if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
                nested = nested.stream()
                        .filter(p -> status.equalsIgnoreCase(String.valueOf(p.getOrDefault("status", ""))))
                        .collect(Collectors.toList());
            }
            // Filter by gender
            if (gender != null && !gender.isBlank() && !"all".equalsIgnoreCase(gender)) {
                nested = nested.stream()
                        .filter(p -> gender.equalsIgnoreCase(String.valueOf(p.getOrDefault("gender", ""))))
                        .collect(Collectors.toList());
            }

            // Manual pagination
            int total = nested.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            List<Map<String, Object>> pageContent = start < total ? nested.subList(start, end) : List.of();
            int totalPages = total > 0 ? (int) Math.ceil((double) total / size) : 0;

            // Build Spring Page-like response
            Map<String, Object> pageResponse = new LinkedHashMap<>();
            pageResponse.put("content", pageContent);
            pageResponse.put("totalElements", total);
            pageResponse.put("totalPages", totalPages);
            pageResponse.put("size", size);
            pageResponse.put("number", page);
            pageResponse.put("numberOfElements", pageContent.size());
            pageResponse.put("first", page == 0);
            pageResponse.put("last", page >= totalPages - 1);
            pageResponse.put("empty", pageContent.isEmpty());

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true).message("Patients retrieved successfully").data(pageResponse).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to retrieve patients", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false).message("Failed to retrieve patients: " + e.getMessage()).build());
        }
    }

    @PostMapping("/api/patients")

    public ResponseEntity<ApiResponse<Map<String, Object>>> createPatient(@RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> flat = nestedToFlat(body);
            // Validate required email field
            String emailVal = flat.get("email") != null ? String.valueOf(flat.get("email")).trim() : "";
            if (emailVal.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Email is required"));
            }
            if (!emailVal.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Enter a valid email address"));
            }
            // Auto-generate MRN if missing
            Object mrn = flat.get("medicalRecordNumber");
            if (mrn == null || String.valueOf(mrn).isBlank()) {
                flat.put("medicalRecordNumber", "MRN-" + System.currentTimeMillis());
            }
            // Ensure patient is active by default (status maps to Patient.active boolean)
            Object status = flat.get("status");
            if (status == null || "Active".equalsIgnoreCase(String.valueOf(status))) {
                flat.put("status", "true");
            } else if ("Inactive".equalsIgnoreCase(String.valueOf(status))) {
                flat.put("status", "false");
            }
            Map<String, Object> created = fhirService.create("demographics", null, flat);
            String fhirId = String.valueOf(created.get("fhirId"));
            Map<String, Object> result = flatToNested(created);
            enrichPatientFields(result);

            // Auto-create Keycloak PATIENT account if email is present
            String email = flat.get("email") != null ? String.valueOf(flat.get("email")).trim() : "";
            if (!email.isBlank()) {
                try {
                    String orgAlias = RequestContext.get().getOrgName();
                    String firstName = flat.get("firstName") != null ? String.valueOf(flat.get("firstName")).trim() : "";
                    String lastName = flat.get("lastName") != null ? String.valueOf(flat.get("lastName")).trim() : "";
                    String tempPassword = keycloakUserService.generateTempPassword();

                    String userId = keycloakUserService.createUser(
                            email, firstName, lastName, tempPassword,
                            Map.of("patient_fhir_id", fhirId));
                    if (userId != null) {
                        keycloakUserService.addUserToOrganization(userId, orgAlias);
                        keycloakUserService.assignRolesToUser(userId, List.of("PATIENT"));
                        log.info("Created Keycloak PATIENT account for patient {} ({})", fhirId, email);
                        result.put("keycloakUserId", userId);
                        result.put("temporaryPassword", tempPassword);
                        result.put("hasAccount", true);
                    }
                } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
                    log.warn("Failed to create Keycloak account for patient {}: {}", fhirId, e.getMessage());
                }
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Patient created successfully", result));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create patient", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create patient: " + e.getMessage()));
        }
    }

    @GetMapping("/api/patients/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPatient(@PathVariable String id) {
        try {
            Map<String, Object> data = fhirService.get("demographics", null, id);
            if (data == null) {
                return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                        .success(false).message("No patient id matches").data(null).build());
            }
            Map<String, Object> result = flatToNested(data);
            enrichPatientFields(result);
            enrichPatientAccountInfo(result);
            return ResponseEntity.ok(ApiResponse.ok("Patient retrieved successfully", result));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to retrieve patient with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("No patient id matches").data(null).build());
        }
    }

    @PutMapping("/api/patients/{id}")

    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePatient(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> flat = nestedToFlat(body);
            // Validate required email field
            String emailVal = flat.get("email") != null ? String.valueOf(flat.get("email")).trim() : "";
            if (emailVal.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Email is required"));
            }
            if (!emailVal.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Enter a valid email address"));
            }
            // Convert human-readable status to FHIR boolean (Patient.active)
            Object status = flat.get("status");
            if (status != null) {
                String s = String.valueOf(status);
                if ("Active".equalsIgnoreCase(s)) {
                    flat.put("status", "true");
                } else if ("Inactive".equalsIgnoreCase(s)) {
                    flat.put("status", "false");
                }
            }
            Map<String, Object> updated = fhirService.update("demographics", null, id, flat);
            Map<String, Object> result = flatToNested(updated);
            enrichPatientFields(result);
            enrichPatientAccountInfo(result);
            return ResponseEntity.ok(ApiResponse.ok("Patient updated successfully", result));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Patient not found with id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Patient not found with id: " + id).build());
        } catch (Exception e) {
            log.error("Failed to update patient with id {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to update patient. Please try again.").build());
        }
    }

    @DeleteMapping("/api/patients/{id}")

    public ResponseEntity<ApiResponse<Void>> deletePatient(@PathVariable String id) {
        try {
            fhirService.delete("demographics", id);
            return ResponseEntity.ok(ApiResponse.ok("Patient deleted successfully", null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete patient with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false).message("No patient id matches").build());
        }
    }

    @GetMapping("/api/patients/count")
    public ResponseEntity<ApiResponse<Long>> getPatientCount() {
        try {
            long count = fhirService.count("demographics");
            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .success(true).message("Patient count retrieved successfully").data(count).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to count patients", e);
            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .success(false).message("Failed to count patients: " + e.getMessage()).build());
        }
    }

    // ── Patient Account Management ──

    @PostMapping("/api/patients/{id}/create-account")

    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPatientAccount(@PathVariable String id) {
        try {
            // Check if account already exists
            Map<String, Object> existing = keycloakUserService.findUserByAttribute("patient_fhir_id", id);
            if (existing != null) {
                return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                        .success(false).message("Patient already has a Keycloak account").data(null).build());
            }

            // Get patient data from FHIR to extract email/name
            Map<String, Object> patientData = fhirService.get("demographics", null, id);
            if (patientData == null) {
                return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                        .success(false).message("Patient not found").data(null).build());
            }

            String email = patientData.get("email") != null ? String.valueOf(patientData.get("email")).trim() : "";
            if (email.isBlank()) {
                return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                        .success(false).message("Patient has no email address. Add an email first.").data(null).build());
            }

            String firstName = patientData.get("firstName") != null ? String.valueOf(patientData.get("firstName")).trim() : "";
            String lastName = patientData.get("lastName") != null ? String.valueOf(patientData.get("lastName")).trim() : "";
            String orgAlias = RequestContext.get().getOrgName();
            String tempPassword = keycloakUserService.generateTempPassword();

            String userId;
            boolean existingUser = false;
            try {
                userId = keycloakUserService.createUser(
                        email, firstName, lastName, tempPassword,
                        Map.of("patient_fhir_id", id));
            } catch (RuntimeException ex) {
                // Handle 409 Conflict: user with same email already exists in Keycloak
                if (ex.getMessage() != null && ex.getMessage().contains("409")) {
                    log.info("Keycloak user with email {} already exists, linking to patient {}", email, id);
                    // Search for existing user by email and link them
                    var existingUsers = keycloakUserService.listUsersByOrg(orgAlias, 0, 100, email);
                    var matchByEmail = existingUsers.stream()
                            .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                            .findFirst().orElse(null);
                    if (matchByEmail != null) {
                        userId = matchByEmail.getId();
                        existingUser = true;
                        // Update the user's patient_fhir_id attribute to link to this patient
                        keycloakUserService.updateUserAttributes(userId, Map.of("patient_fhir_id", id));
                    } else {
                        // User exists in realm but not in this org — try searching all users
                        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                                .success(false)
                                .message("A user with email " + email + " already exists in the system. Please contact support to link this patient.")
                                .data(null).build());
                    }
                } else {
                    throw ex;
                }
            }
            if (userId != null) {
                if (!existingUser) {
                    keycloakUserService.addUserToOrganization(userId, orgAlias);
                }
                keycloakUserService.assignRolesToUser(userId, List.of("PATIENT"));
                log.info("{}Keycloak PATIENT account for patient {} ({})", existingUser ? "Linked existing " : "Created ", id, email);

                Map<String, Object> data = new LinkedHashMap<>();
                data.put("userId", userId);
                data.put("username", email);
                data.put("temporaryPassword", existingUser ? null : tempPassword);
                data.put("resetDate", LocalDate.now().toString());
                data.put("existingAccount", existingUser);
                return ResponseEntity.ok(ApiResponse.ok(
                        existingUser ? "Existing account linked to patient" : "Patient account created", data));
            }
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to create account").data(null).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create account for patient {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to create account: " + e.getMessage()).data(null).build());
        }
    }

    @PostMapping("/api/patients/{id}/reset-password")

    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetPatientPassword(@PathVariable String id) {
        try {
            Map<String, Object> kcUser = keycloakUserService.findUserByAttribute("patient_fhir_id", id);
            if (kcUser == null) {
                return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                        .success(false).message("No Keycloak account linked to this patient").data(null).build());
            }
            String userId = (String) kcUser.get("id");
            String tempPassword = keycloakUserService.resetPassword(userId);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("userId", userId);
            data.put("username", kcUser.get("email"));
            data.put("temporaryPassword", tempPassword);
            data.put("resetDate", LocalDate.now().toString());
            return ResponseEntity.ok(ApiResponse.ok("Password reset successfully", data));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to reset password for patient {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to reset password: " + e.getMessage()).data(null).build());
        }
    }

    @PostMapping("/api/patients/{id}/send-reset-email")

    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Void>> sendPatientResetEmail(@PathVariable String id) {
        try {
            Map<String, Object> kcUser = keycloakUserService.findUserByAttribute("patient_fhir_id", id);
            if (kcUser == null) {
                return ResponseEntity.ok(ApiResponse.<Void>builder()
                        .success(false).message("No Keycloak account linked to this patient").data(null).build());
            }
            String kcUserId = (String) kcUser.get("id");

            // Try Keycloak built-in email first
            try {
                keycloakUserService.sendPasswordResetEmail(kcUserId);
                return ResponseEntity.ok(ApiResponse.ok("Password reset email sent", null));
            } catch (Exception keycloakEx) {
                log.warn("Keycloak email failed for patient {}, falling back to app EmailService: {}", id, keycloakEx.getMessage());
            }

            // Fallback: reset password and send via app's EmailService
            String email = (String) kcUser.get("email");
            if (email == null || email.isBlank()) {
                return ResponseEntity.ok(ApiResponse.<Void>builder()
                        .success(false).message("Patient has no email address configured").data(null).build());
            }
            String tempPassword = keycloakUserService.resetPassword(kcUserId);
            String username = (String) kcUser.get("username");
            String firstName = (String) kcUser.get("firstName");
            String orgAlias = RequestContext.get().getOrgName();
            String htmlBody = buildResetEmailHtml(firstName, username, tempPassword);
            emailService.sendEmail(orgAlias, email, "Password Reset - Your Temporary Credentials", htmlBody);
            return ResponseEntity.ok(ApiResponse.ok("Password reset email sent", null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to send reset email for patient {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false).message("Failed to send reset email: " + e.getMessage()).data(null).build());
        }
    }

    @GetMapping("/api/patients/{id}/account-status")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPatientAccountStatus(@PathVariable String id) {
        try {
            Map<String, Object> kcUser = keycloakUserService.findUserByAttribute("patient_fhir_id", id);
            Map<String, Object> data = new LinkedHashMap<>();
            if (kcUser != null) {
                data.put("hasAccount", true);
                data.put("keycloakUserId", kcUser.get("id"));
                data.put("email", kcUser.get("email"));
                data.put("accountEnabled", kcUser.getOrDefault("enabled", false));
            } else {
                data.put("hasAccount", false);
            }
            return ResponseEntity.ok(ApiResponse.ok("Account status retrieved", data));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get account status for patient {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to get account status").data(null).build());
        }
    }

    @PutMapping("/api/patients/{id}/toggle-account")

    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> togglePatientAccount(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> kcUser = keycloakUserService.findUserByAttribute("patient_fhir_id", id);
            if (kcUser == null) {
                return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                        .success(false).message("No Keycloak account linked to this patient").data(null).build());
            }
            String userId = (String) kcUser.get("id");
            boolean enable = Boolean.TRUE.equals(body.get("enabled"));
            if (enable) {
                keycloakUserService.enableUser(userId);
            } else {
                keycloakUserService.disableUser(userId);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("userId", userId);
            data.put("enabled", enable);
            return ResponseEntity.ok(ApiResponse.ok(enable ? "Account enabled" : "Account blocked", data));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to toggle account for patient {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to toggle account: " + e.getMessage()).data(null).build());
        }
    }

    // ==================== COVERAGES (tab: insurance) ====================

    @GetMapping("/api/coverages")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listAllCoverages() {
        try {
            Map<String, Object> data = fhirService.listAll("insurance", 0, 200);
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                    .success(true).message("Coverages retrieved successfully").data(content).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to search all Coverages", e);
            return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                    .success(false).message("Failed to retrieve coverages: " + e.getMessage()).data(null).build());
        }
    }

    @GetMapping("/api/coverages/{patientId}")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCoverageByPatient(@PathVariable Long patientId) {
        try {
            Map<String, Object> data = fhirService.list("insurance", patientId, 0, 100);
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            if (content.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                        .success(false).message("No coverage found for patient ID: " + patientId).build());
            }
            return ResponseEntity.ok(ApiResponse.ok("Coverage retrieved successfully", content.get(0)));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to retrieve Coverage for patientId {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("No coverage found for patient ID: " + patientId).build());
        }
    }

    @PostMapping("/api/coverages")

    public ResponseEntity<ApiResponse<Map<String, Object>>> createCoverage(@RequestBody Map<String, Object> body) {
        try {
            Long patientId = toLong(body.get("patientId"));
            Map<String, Object> created = fhirService.create("insurance", patientId, body);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Coverage created successfully", created));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create Coverage", e);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to create Coverage: " + e.getMessage()).build());
        }
    }

    @PutMapping("/api/coverages/{patientId}")

    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCoverageByPatient(
            @PathVariable Long patientId, @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> data = fhirService.list("insurance", patientId, 0, 100);
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            if (content.isEmpty()) {
                // Create if none exists
                Map<String, Object> created = fhirService.create("insurance", patientId, body);
                return ResponseEntity.ok(ApiResponse.ok("Coverage created successfully", created));
            }
            String resourceId = String.valueOf(content.get(0).get("fhirId"));
            Map<String, Object> updated = fhirService.update("insurance", patientId, resourceId, body);
            return ResponseEntity.ok(ApiResponse.ok("Coverage updated successfully", updated));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update Coverage for patientId {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to update Coverage: " + e.getMessage()).build());
        }
    }

    @DeleteMapping("/api/coverages/{patientId}")

    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Void>> deleteCoverageByPatient(@PathVariable Long patientId) {
        try {
            Map<String, Object> data = fhirService.list("insurance", patientId, 0, 100);
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            for (Map<String, Object> c : content) {
                String fhirId = String.valueOf(c.get("fhirId"));
                fhirService.delete("insurance", fhirId);
            }
            return ResponseEntity.ok(ApiResponse.ok("Coverage deleted successfully", null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete Coverage for patientId {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false).message("Failed to delete Coverage: " + e.getMessage()).build());
        }
    }

    @GetMapping("/api/coverages/{patientId}/{coverageId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCoverageItem(
            @PathVariable Long patientId, @PathVariable String coverageId) {
        try {
            Map<String, Object> item = fhirService.get("insurance", patientId, coverageId);
            return ResponseEntity.ok(ApiResponse.ok("Coverage retrieved successfully", item));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to retrieve coverage {} for patient {}", coverageId, patientId, e);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to retrieve coverage: " + e.getMessage()).build());
        }
    }

    @PutMapping("/api/coverages/{patientId}/{coverageId}")

    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCoverageItem(
            @PathVariable Long patientId, @PathVariable String coverageId, @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> updated = fhirService.update("insurance", patientId, coverageId, body);
            return ResponseEntity.ok(ApiResponse.ok("Coverage updated successfully", updated));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update coverage {} for patient {}", coverageId, patientId, e);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to update coverage: " + e.getMessage()).build());
        }
    }

    @DeleteMapping("/api/coverages/{patientId}/{coverageId}")

    public ResponseEntity<ApiResponse<Void>> deleteCoverageItem(
            @PathVariable Long patientId, @PathVariable String coverageId) {
        try {
            fhirService.delete("insurance", coverageId);
            return ResponseEntity.ok(ApiResponse.ok("Coverage deleted successfully", null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete coverage {} for patient {}", coverageId, patientId, e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false).message("Failed to delete coverage: " + e.getMessage()).build());
        }
    }

    // ==================== RECALLS — Moved to RecallController (recall package) ====================

    // ==================== ENCOUNTERS (tab: encounters) ====================

    @GetMapping("/api/{patientId}/encounters")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listEncountersByPatient(
            @PathVariable Long patientId) {
        try {
            Map<String, Object> data = fhirService.list("encounters", patientId, 0, 100);
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            List<Map<String, Object>> enriched = content.stream()
                    .map(FhirFacadeController::enrichEncounterFields)
                    .toList();
            return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                    .success(true).message("Encounters fetched").data(enriched).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(ApiResponse.<List<Map<String, Object>>>builder()
                    .success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error fetching encounters for patient {}", patientId, ex);
            return ResponseEntity.status(500).body(ApiResponse.<List<Map<String, Object>>>builder()
                    .success(false).message("Error fetching encounters: " + ex.getMessage()).build());
        }
    }

    @PostMapping("/api/{patientId}/encounters")

    public ResponseEntity<ApiResponse<Map<String, Object>>> createEncounter(
            @PathVariable Long patientId, @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> created = fhirService.create("encounters", patientId, body);
            return ResponseEntity.ok(ApiResponse.ok("Encounter created", enrichEncounterFields(created)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error creating encounter for patient {}", patientId, ex);
            return ResponseEntity.status(500).body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Error creating Encounter: " + ex.getMessage()).build());
        }
    }

    @GetMapping("/api/{patientId}/encounters/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEncounterForPatient(
            @PathVariable Long patientId, @PathVariable String id) {
        try {
            Map<String, Object> data = fhirService.get("encounters", patientId, id);
            if (data == null) {
                return ResponseEntity.status(404).body(ApiResponse.<Map<String, Object>>builder()
                        .success(false).message("Encounter not found: " + id).build());
            }
            return ResponseEntity.ok(ApiResponse.ok("Encounter fetched", enrichEncounterFields(data)));
        } catch (Exception ex) {
            log.error("Error fetching encounter {} for patient {}", id, patientId, ex);
            return ResponseEntity.status(500).body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Error fetching encounter: " + ex.getMessage()).build());
        }
    }

    @PutMapping("/api/{patientId}/encounters/{id}")

    public ResponseEntity<ApiResponse<Map<String, Object>>> updateEncounter(
            @PathVariable Long patientId, @PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            // Check if encounter is signed
            checkEncounterNotSigned(id);
            Map<String, Object> updated = fhirService.update("encounters", patientId, id, body);
            return ResponseEntity.ok(ApiResponse.ok("Encounter updated", enrichEncounterFields(updated)));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423).body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error updating encounter {} for patient {}", id, patientId, ex);
            return ResponseEntity.status(500).body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Error updating encounter: " + ex.getMessage()).build());
        }
    }

    @DeleteMapping("/api/{patientId}/encounters/{id}")

    public ResponseEntity<ApiResponse<Void>> deleteEncounter(
            @PathVariable Long patientId, @PathVariable String id) {
        try {
            checkEncounterNotSigned(id);
            fhirService.delete("encounters", id);
            return ResponseEntity.ok(ApiResponse.ok("Encounter deleted successfully", null));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423).body(ApiResponse.<Void>builder()
                    .success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error deleting encounter {} for patient {}", id, patientId, ex);
            return ResponseEntity.status(500).body(ApiResponse.<Void>builder()
                    .success(false).message("Error deleting encounter: " + ex.getMessage()).build());
        }
    }

    @PostMapping("/api/{patientId}/encounters/{id}/sign")

    public ResponseEntity<ApiResponse<Map<String, Object>>> signEncounter(
            @PathVariable Long patientId, @PathVariable String id) {
        try {
            return updateEncounterStatus(id, Encounter.EncounterStatus.FINISHED, "Encounter signed");
        } catch (Exception ex) {
            log.error("Error signing encounter {}", id, ex);
            return ResponseEntity.status(400).body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Error signing encounter: " + ex.getMessage()).build());
        }
    }

    @PostMapping("/api/{patientId}/encounters/{id}/unsign")

    public ResponseEntity<ApiResponse<Map<String, Object>>> unsignEncounter(
            @PathVariable Long patientId, @PathVariable String id) {
        try {
            return updateEncounterStatus(id, Encounter.EncounterStatus.INPROGRESS, "Encounter unsigned");
        } catch (Exception ex) {
            log.error("Error unsigning encounter {}", id, ex);
            return ResponseEntity.status(400).body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Error unsigning encounter: " + ex.getMessage()).build());
        }
    }

    @PostMapping("/api/{patientId}/encounters/{id}/incomplete")

    public ResponseEntity<ApiResponse<Map<String, Object>>> markEncounterIncomplete(
            @PathVariable Long patientId, @PathVariable String id) {
        try {
            return updateEncounterStatus(id, Encounter.EncounterStatus.TRIAGED, "Encounter marked incomplete");
        } catch (Exception ex) {
            log.error("Error marking encounter incomplete {}", id, ex);
            return ResponseEntity.status(400).body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Error marking encounter incomplete: " + ex.getMessage()).build());
        }
    }

    // Org-wide encounter browse
    @GetMapping("/api/encounters")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> browseEncounters(
            @RequestParam(name = "status", defaultValue = "ALL") String status,
            @RequestParam(name = "recentOnly", defaultValue = "false") boolean recentOnly,
            @RequestParam(name = "recentCount", defaultValue = "10") int recentCount,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        try {
            Map<String, Object> data = fhirService.listAll("encounters", 0, 500);
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            List<Map<String, Object>> enriched = content.stream()
                    .map(FhirFacadeController::enrichEncounterFields)
                    .collect(Collectors.toList());

            // Filter by status
            String normalized = status == null ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
            if (!"ALL".equals(normalized)) {
                enriched = enriched.stream()
                        .filter(e -> normalized.equals(String.valueOf(e.getOrDefault("status", "")).toUpperCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }

            // Sort by encounterDate descending (most recent first)
            enriched.sort((a, b) -> {
                String da = String.valueOf(a.getOrDefault("encounterDate", ""));
                String db = String.valueOf(b.getOrDefault("encounterDate", ""));
                return db.compareTo(da);
            });

            if (recentOnly) {
                enriched = enriched.subList(0, Math.min(recentCount, enriched.size()));
            }

            // Paginate
            int total = enriched.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            List<Map<String, Object>> pageContent = start < total ? enriched.subList(start, end) : List.of();
            int totalPages = total > 0 ? (int) Math.ceil((double) total / size) : 0;

            Map<String, Object> pageResponse = new LinkedHashMap<>();
            pageResponse.put("content", pageContent);
            pageResponse.put("totalElements", total);
            pageResponse.put("totalPages", totalPages);
            pageResponse.put("size", size);
            pageResponse.put("number", page);
            pageResponse.put("numberOfElements", pageContent.size());
            pageResponse.put("first", page == 0);
            pageResponse.put("last", page >= totalPages - 1);
            pageResponse.put("empty", pageContent.isEmpty());

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true).message("Encounters fetched").data(pageResponse).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to browse encounters", e);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true).message("Encounters fetched").data(emptyPage(page, size)).build());
        }
    }

    @GetMapping("/api/encounters/report/encounterAll")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllEncountersReport() {
        try {
            Map<String, Object> data = fhirService.listAll("encounters", 0, 1000);
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            List<Map<String, Object>> enriched = content.stream()
                    .map(FhirFacadeController::enrichEncounterFields).toList();
            return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                    .success(true).message("All encounters fetched").data(enriched).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get all encounters report", e);
            return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                    .success(true).message("All encounters fetched").data(List.of()).build());
        }
    }

    // ==================== PROCEDURES (tab: procedures) ====================

    @GetMapping("/api/procedures/{patientId}")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listProceduresByPatient(
            @PathVariable Long patientId) {
        try {
            Map<String, Object> data = fhirService.list("procedures", patientId, 0, 100);
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                    .success(true).message("Procedures fetched successfully").data(content).build());
        } catch (Exception ex) {
            log.error("Error fetching Procedures for Patient {}", patientId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<Map<String, Object>>>builder()
                            .success(false).message("Error fetching Procedures: " + ex.getMessage()).build());
        }
    }

    @GetMapping("/api/procedures/{patientId}/{encounterId}")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listProceduresByEncounter(
            @PathVariable Long patientId, @PathVariable String encounterId) {
        try {
            Map<String, Object> data = fhirService.list("procedures", patientId, 0, 100, encounterId);
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                    .success(true).message("Procedures fetched successfully").data(content).build());
        } catch (Exception ex) {
            log.error("Error fetching Procedures for Patient {} Encounter {}", patientId, encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<Map<String, Object>>>builder()
                            .success(false).message("Error fetching Procedures: " + ex.getMessage()).build());
        }
    }

    @GetMapping("/api/procedures/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOneProcedure(
            @PathVariable Long patientId, @PathVariable String encounterId, @PathVariable String id) {
        try {
            Map<String, Object> data = fhirService.get("procedures", patientId, id);
            if (data == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Procedure not found"));
            }
            return ResponseEntity.ok(ApiResponse.ok("Procedure fetched successfully", data));
        } catch (Exception ex) {
            log.error("Error fetching Procedure {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error fetching Procedure: " + ex.getMessage()));
        }
    }

    @PostMapping("/api/procedures/{patientId}/{encounterId}")

    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Object>> createProcedure(
            @PathVariable Long patientId, @PathVariable String encounterId, @RequestBody Object requestBody) {
        try {
            if (requestBody instanceof List<?> items) {
                // Multi-item: create each item as a separate Procedure
                List<Map<String, Object>> results = new ArrayList<>();
                for (Object item : items) {
                    if (item instanceof Map) {
                        Map<String, Object> formData = (Map<String, Object>) item;
                        Map<String, Object> created = fhirService.create("procedures", patientId, formData, encounterId);
                        results.add(created);
                    }
                }
                return ResponseEntity.ok(ApiResponse.<Object>builder()
                        .success(true).message("Procedure created").data(results).build());
            } else if (requestBody instanceof Map) {
                Map<String, Object> formData = (Map<String, Object>) requestBody;
                // Check for nested codeItems
                Object codeItems = formData.get("codeItems");
                if (codeItems instanceof List<?> items && !items.isEmpty()) {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (Object item : items) {
                        if (item instanceof Map) {
                            Map<String, Object> ci = new LinkedHashMap<>((Map<String, Object>) item);
                            // Copy common fields from parent
                            for (String key : List.of("hospitalBillingStart", "hospitalBillingEnd", "note", "priceLevelId", "priceLevelTitle", "providername")) {
                                if (formData.containsKey(key) && !ci.containsKey(key)) {
                                    ci.put(key, formData.get(key));
                                }
                            }
                            Map<String, Object> created = fhirService.create("procedures", patientId, ci, encounterId);
                            results.add(created);
                        }
                    }
                    return ResponseEntity.ok(ApiResponse.<Object>builder()
                            .success(true).message("Procedure created").data(results).build());
                }
                Map<String, Object> created = fhirService.create("procedures", patientId, formData, encounterId);
                return ResponseEntity.ok(ApiResponse.<Object>builder()
                        .success(true).message("Procedure created").data(created).build());
            }
            return ResponseEntity.badRequest().body(ApiResponse.<Object>builder()
                    .success(false).message("Invalid request body").build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.<Object>builder()
                    .success(false).message(e.getMessage()).build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423).body(ApiResponse.<Object>builder()
                    .success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error creating Procedure for Patient {} Encounter {}", patientId, encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Object>builder()
                            .success(false).message("Error creating Procedure: " + ex.getMessage()).build());
        }
    }

    @PutMapping("/api/procedures/{patientId}/{encounterId}/{id}")

    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProcedure(
            @PathVariable Long patientId, @PathVariable String encounterId, @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> updated = fhirService.update("procedures", patientId, id, body);
            return ResponseEntity.ok(ApiResponse.ok("Procedure updated successfully", updated));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception ex) {
            log.error("Error updating Procedure {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error updating Procedure: " + ex.getMessage()));
        }
    }

    @DeleteMapping("/api/procedures/{patientId}/{encounterId}/{id}")

    public ResponseEntity<ApiResponse<Void>> deleteProcedure(
            @PathVariable Long patientId, @PathVariable String encounterId, @PathVariable String id) {
        try {
            fhirService.delete("procedures", id);
            return ResponseEntity.ok(ApiResponse.ok("Procedure deleted successfully", null));
        } catch (Exception ex) {
            log.error("Error deleting Procedure {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>builder()
                            .success(false).message("Error deleting Procedure: " + ex.getMessage()).build());
        }
    }

    // ==================== SCHEDULES ====================

    @GetMapping("/api/schedules")
    public ResponseEntity<ApiResponse<List<ScheduleDto>>> listSchedules() {
        return ResponseEntity.ok(scheduleService.getAllSchedules());
    }

    @PostMapping("/api/schedules")

    public ResponseEntity<ApiResponse<ScheduleDto>> createSchedule(@RequestBody ScheduleDto dto) {
        try {
            ScheduleDto created = scheduleService.create(dto);
            return ResponseEntity.ok(ApiResponse.<ScheduleDto>builder()
                    .success(true).message("Schedule created").data(created).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create schedule", e);
            return ResponseEntity.ok(ApiResponse.<ScheduleDto>builder()
                    .success(false).message("Failed to create schedule: " + e.getMessage()).build());
        }
    }

    @GetMapping("/api/schedules/{id}")
    public ResponseEntity<ApiResponse<ScheduleDto>> getSchedule(@PathVariable String id) {
        try {
            ScheduleDto schedule = scheduleService.getById(id);
            return ResponseEntity.ok(ApiResponse.<ScheduleDto>builder()
                    .success(true).message("Schedule retrieved").data(schedule).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get schedule {}", id, e);
            return ResponseEntity.ok(ApiResponse.<ScheduleDto>builder()
                    .success(false).message("Failed to get schedule: " + e.getMessage()).build());
        }
    }

    @PutMapping("/api/schedules/{id}")

    public ResponseEntity<ApiResponse<ScheduleDto>> updateSchedule(
            @PathVariable String id, @RequestBody ScheduleDto dto) {
        try {
            ScheduleDto updated = scheduleService.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<ScheduleDto>builder()
                    .success(true).message("Schedule updated").data(updated).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update schedule {}", id, e);
            return ResponseEntity.ok(ApiResponse.<ScheduleDto>builder()
                    .success(false).message("Failed to update schedule: " + e.getMessage()).build());
        }
    }

    @DeleteMapping("/api/schedules/{id}")

    public ResponseEntity<ApiResponse<Void>> deleteSchedule(@PathVariable String id) {
        try {
            scheduleService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Schedule deleted", null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete schedule {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false).message("Failed to delete schedule: " + e.getMessage()).build());
        }
    }

    // ==================== SLOTS ====================

    @GetMapping("/api/slots")
    public ResponseEntity<ApiResponse<?>> listSlots() {
        return ResponseEntity.ok(slotService.getAllSlots());
    }

    @PostMapping("/api/slots")

    public ResponseEntity<ApiResponse<?>> createSlot(@RequestBody org.ciyex.ehr.dto.SlotDto dto) {
        try {
            var created = slotService.create(dto);
            return ResponseEntity.ok(ApiResponse.builder().success(true).message("Slot created").data(created).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create slot", e);
            return ResponseEntity.ok(ApiResponse.builder().success(false).message("Failed: " + e.getMessage()).build());
        }
    }

    @GetMapping("/api/slots/{id}")
    public ResponseEntity<ApiResponse<?>> getSlot(@PathVariable String id) {
        try {
            var slot = slotService.getById(id);
            return ResponseEntity.ok(ApiResponse.builder().success(true).message("Slot retrieved").data(slot).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.builder().success(false).message("Failed: " + e.getMessage()).build());
        }
    }

    @PutMapping("/api/slots/{id}")

    public ResponseEntity<ApiResponse<?>> updateSlot(@PathVariable String id, @RequestBody org.ciyex.ehr.dto.SlotDto dto) {
        try {
            var updated = slotService.update(id, dto);
            return ResponseEntity.ok(ApiResponse.builder().success(true).message("Slot updated").data(updated).build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.builder().success(false).message("Failed: " + e.getMessage()).build());
        }
    }

    @DeleteMapping("/api/slots/{id}")

    public ResponseEntity<ApiResponse<Void>> deleteSlot(@PathVariable String id) {
        try {
            slotService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Slot deleted", null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.<Void>builder().success(false).message("Failed: " + e.getMessage()).build());
        }
    }

    // ==================== APPOINTMENTS (tab: appointments) ====================

    @GetMapping("/api/appointments")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        try {
            Map<String, Object> data = fhirService.listAll("appointments", page, size, "date", dateFrom, dateTo);
            transformAppointmentList(data, status);

            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            if (!content.isEmpty()) {
                List<String> apptIds = content.stream()
                        .map(a -> String.valueOf(a.get("id")))
                        .filter(id -> id != null && !"null".equals(id))
                        .toList();
                Map<String, String[]> encounterMap = appointmentEncounterService.findEncounterMapForAppointments(apptIds);
                for (Map<String, Object> appt : content) {
                    String apptId = String.valueOf(appt.get("id"));
                    String[] enc = encounterMap.get(apptId);
                    if (enc != null) {
                        appt.put("encounterId", enc[0]);
                        if (enc[1] != null) appt.put("encounterPatientId", Long.parseLong(enc[1]));
                    }
                }
            }

            return ResponseEntity.ok(ApiResponse.ok("Appointments retrieved", data));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to list appointments", e);
            return ResponseEntity.ok(ApiResponse.ok("Appointments retrieved", emptyPage(page, size)));
        }
    }

    @PostMapping("/api/appointments")

    public ResponseEntity<ApiResponse<Map<String, Object>>> createAppointment(@RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> formData = transformAppointmentToFhir(body);
            Long patientId = toLong(body.get("patientId"));
            Map<String, Object> created = fhirService.create("appointments", patientId, formData);
            Map<String, Object> response = transformAppointmentFromFhir(created);

            // Fire appointment confirmation notification asynchronously
            try {
                String orgAlias = RequestContext.get().getOrgName();
                Map<String, Object> notifData = new HashMap<>(body);
                notifData.putAll(response);
                // Look up patient email now (in request thread where context is available)
                if (patientId != null && !notifData.containsKey("patientEmail")) {
                    try {
                        Map<String, Object> patientData = fhirService.get("demographics", patientId, null);
                        if (patientData != null) {
                            Object email = patientData.get("email");
                            if (email != null && !String.valueOf(email).isBlank()) {
                                notifData.put("patientEmail", String.valueOf(email));
                            }
                            // Also try telecom array
                            if (!notifData.containsKey("patientEmail") && patientData.get("telecom") instanceof java.util.List<?> telecoms) {
                                for (Object t : telecoms) {
                                    if (t instanceof Map<?, ?> tm && "email".equals(tm.get("system"))) {
                                        notifData.put("patientEmail", String.valueOf(tm.get("value")));
                                        break;
                                    }
                                }
                            }
                            String fn = patientData.get("firstName") != null ? String.valueOf(patientData.get("firstName")) : "";
                            String ln = patientData.get("lastName") != null ? String.valueOf(patientData.get("lastName")) : "";
                            String name = (fn + " " + ln).trim();
                            if (!name.isBlank()) notifData.putIfAbsent("patientName", name);
                        }
                    } catch (Exception e) {
                        log.debug("Could not look up patient {} for notification: {}", patientId, e.getMessage());
                    }
                }
                appointmentNotificationService.onAppointmentCreated(orgAlias, notifData);
            } catch (Exception notifEx) {
                log.warn("Failed to trigger appointment notification: {}", notifEx.getMessage());
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Appointment created", response));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create appointment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create appointment: " + e.getMessage()));
        }
    }

    @PutMapping("/api/appointments/{id}")

    public ResponseEntity<ApiResponse<Map<String, Object>>> updateAppointment(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> formData = transformAppointmentToFhir(body);
            Long patientId = toLong(body.get("patientId"));
            Map<String, Object> updated = fhirService.update("appointments", patientId, id, formData);
            return ResponseEntity.ok(ApiResponse.ok("Appointment updated", transformAppointmentFromFhir(updated)));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update appointment {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update appointment: " + e.getMessage()));
        }
    }

    @PutMapping("/api/appointments/{id}/status")

    public ResponseEntity<ApiResponse<Map<String, Object>>> updateAppointmentStatus(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> existing = fhirService.get("appointments", null, id);
            if (existing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Appointment not found: " + id));
            }
            String newStatus = String.valueOf(body.get("status"));
            existing.put("status", newStatus);
            Map<String, Object> updated = fhirService.update("appointments", null, id, existing);
            Map<String, Object> response = transformAppointmentFromFhir(updated);
            String encounterId = appointmentEncounterService.handleStatusChange(id, newStatus, existing);
            if (encounterId != null) {
                response.put("encounterId", encounterId);
                Long patientId = appointmentEncounterService.getPatientIdFromAppointment(existing);
                if (patientId != null) response.put("encounterPatientId", patientId);
            }
            return ResponseEntity.ok(ApiResponse.ok("Status updated", response));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update appointment status {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update status: " + e.getMessage()));
        }
    }

    @PostMapping("/api/appointments/{id}/encounter")

    public ResponseEntity<ApiResponse<Map<String, Object>>> createEncounterForAppointment(@PathVariable String id) {
        try {
            Map<String, Object> existing = fhirService.get("appointments", null, id);
            if (existing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Appointment not found: " + id));
            }
            String existingEncId = appointmentEncounterService.findEncounterForAppointment(id);
            if (existingEncId != null) {
                Long patientId = appointmentEncounterService.getPatientIdFromAppointment(existing);
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("encounterId", existingEncId);
                if (patientId != null) resp.put("encounterPatientId", patientId);
                return ResponseEntity.ok(ApiResponse.ok("Encounter already exists", resp));
            }
            String encounterId = appointmentEncounterService.createEncounterForAppointment(existing, "Manual encounter");
            if (encounterId == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Failed to create encounter"));
            }
            Long patientId = appointmentEncounterService.getPatientIdFromAppointment(existing);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("encounterId", encounterId);
            if (patientId != null) resp.put("encounterPatientId", patientId);
            return ResponseEntity.ok(ApiResponse.ok("Encounter created", resp));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create encounter for appointment {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create encounter: " + e.getMessage()));
        }
    }

    @GetMapping("/api/appointments/status-options")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAppointmentStatusOptions() {
        List<AppointmentEncounterService.StatusOption> config = appointmentEncounterService.getStatusConfig();
        if (config.isEmpty()) {
            List<Map<String, Object>> fallback = List.of(
                    Map.of("value", "Scheduled", "label", "Scheduled", "order", 0, "nextStatus", "Confirmed"),
                    Map.of("value", "Confirmed", "label", "Confirmed", "order", 1, "nextStatus", "Checked-in"),
                    Map.of("value", "Checked-in", "label", "Checked-in", "order", 2, "nextStatus", "Completed"),
                    Map.of("value", "Completed", "label", "Completed", "order", 3, "terminal", true),
                    Map.of("value", "Re-Scheduled", "label", "Re-Scheduled", "order", 4, "nextStatus", "Scheduled"),
                    Map.of("value", "No Show", "label", "No Show", "order", 5, "terminal", true),
                    Map.of("value", "Cancelled", "label", "Cancelled", "order", 6, "terminal", true));
            return ResponseEntity.ok(ApiResponse.ok("Status options", fallback));
        }
        List<Map<String, Object>> options = config.stream()
                .sorted(Comparator.comparingInt(AppointmentEncounterService.StatusOption::order))
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("value", s.value()); m.put("label", s.label()); m.put("color", s.color());
                    m.put("triggersEncounter", s.triggersEncounter()); m.put("terminal", s.terminal());
                    m.put("order", s.order());
                    if (s.nextStatus() != null) m.put("nextStatus", s.nextStatus());
                    if (s.encounterNote() != null) m.put("encounterNote", s.encounterNote());
                    return m;
                }).toList();
        return ResponseEntity.ok(ApiResponse.ok("Status options", options));
    }

    @GetMapping("/api/appointments/room-options")
    public ResponseEntity<ApiResponse<List<String>>> getAppointmentRoomOptions() {
        return ResponseEntity.ok(ApiResponse.ok("Room options", appointmentEncounterService.getRoomOptions()));
    }

    @PutMapping("/api/appointments/{id}/room")

    public ResponseEntity<ApiResponse<Map<String, Object>>> updateAppointmentRoom(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> existing = fhirService.get("appointments", null, id);
            if (existing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Appointment not found: " + id));
            }
            String room = body.get("room") != null ? String.valueOf(body.get("room")) : null;
            if (room != null && !room.isEmpty()) existing.put("room", room); else existing.remove("room");
            Map<String, Object> updated = fhirService.update("appointments", null, id, existing);
            return ResponseEntity.ok(ApiResponse.ok("Room updated", transformAppointmentFromFhir(updated)));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update appointment room {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update room: " + e.getMessage()));
        }
    }

    @GetMapping("/api/appointments/{id}/encounter")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAppointmentEncounter(@PathVariable String id) {
        try {
            String encounterId = appointmentEncounterService.findEncounterForAppointment(id);
            if (encounterId == null) {
                return ResponseEntity.ok(ApiResponse.ok("No encounter", Map.of("encounterId", (Object) "")));
            }
            Map<String, Object> existing = fhirService.get("appointments", null, id);
            Long patientId = existing != null ? appointmentEncounterService.getPatientIdFromAppointment(existing) : null;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("encounterId", encounterId);
            if (patientId != null) result.put("patientId", patientId);
            return ResponseEntity.ok(ApiResponse.ok("Encounter found", result));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to find encounter for appointment {}", id, e);
            return ResponseEntity.ok(ApiResponse.ok("No encounter", Map.of("encounterId", (Object) "")));
        }
    }

    @GetMapping("/api/appointments/count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> countAppointments() {
        try {
            long count = fhirService.count("appointments");
            return ResponseEntity.ok(ApiResponse.ok("Appointment count", Map.of("count", (Object) count)));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to count appointments", e);
            return ResponseEntity.ok(ApiResponse.ok("Appointment count", Map.of("count", (Object) 0)));
        }
    }

    @GetMapping("/api/appointments/patient/{patientId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listAppointmentsByPatient(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Map<String, Object> data = fhirService.list("appointments", patientId, page, size);
            transformAppointmentList(data, null);
            return ResponseEntity.ok(ApiResponse.ok("Patient appointments retrieved", data));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to list appointments for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.ok("Patient appointments", emptyPage(page, size)));
        }
    }

    @DeleteMapping("/api/appointments/{id}")

    public ResponseEntity<ApiResponse<Void>> deleteAppointment(@PathVariable String id) {
        try {
            fhirService.delete("appointments", id);
            return ResponseEntity.ok(ApiResponse.ok("Appointment deleted", null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete appointment {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete appointment: " + e.getMessage()));
        }
    }

    // ==================== COMMUNICATIONS (tab: messaging) ====================

    @GetMapping("/api/communications")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listCommunications(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        try {
            Map<String, Object> data = fhirService.listAll("messaging", page, size);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            return ResponseEntity.ok(ApiResponse.ok("Communications retrieved", content));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to list communications", e);
            return ResponseEntity.ok(ApiResponse.ok("Communications retrieved", List.of()));
        }
    }

    @PostMapping("/api/communications")

    public ResponseEntity<ApiResponse<Map<String, Object>>> createCommunication(@RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> created = fhirService.create("messaging", null, body);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Communication created", created));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create communication", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create communication: " + e.getMessage()));
        }
    }

    @PutMapping("/api/communications/{id}/archive")

    public ResponseEntity<ApiResponse<Map<String, Object>>> archiveCommunication(@PathVariable String id) {
        try {
            Map<String, Object> existing = fhirService.get("messaging", null, id);
            if (existing == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Communication not found: " + id));
            existing.put("status", "completed");
            Map<String, Object> updated = fhirService.update("messaging", null, id, existing);
            return ResponseEntity.ok(ApiResponse.ok("Communication archived", updated));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to archive communication {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to archive communication: " + e.getMessage()));
        }
    }

    // ==================== TEMPLATES (tab: template-documents) ====================

    @GetMapping("/api/templates")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listTemplates(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        try {
            Map<String, Object> data = fhirService.listAll("template-documents", page, size);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            return ResponseEntity.ok(ApiResponse.ok("Templates retrieved", content));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to list templates", e);
            return ResponseEntity.ok(ApiResponse.ok("Templates retrieved", List.of()));
        }
    }

    @PostMapping("/api/templates")

    public ResponseEntity<ApiResponse<Map<String, Object>>> createTemplate(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Template created", fhirService.create("template-documents", null, body)));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Failed to create template: " + e.getMessage()));
        }
    }

    @PutMapping("/api/templates/{id}")

    public ResponseEntity<ApiResponse<Map<String, Object>>> updateTemplate(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Template updated", fhirService.update("template-documents", null, id, body)));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update template {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Failed to update template: " + e.getMessage()));
        }
    }

    @DeleteMapping("/api/templates/{id}")

    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable String id) {
        try {
            fhirService.delete("template-documents", id);
            return ResponseEntity.ok(ApiResponse.ok("Template deleted", null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete template {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Failed to delete template: " + e.getMessage()));
        }
    }

    // ==================== VITALS (tab: vitals) ====================

    @GetMapping("/api/vitals/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listVitals(
            @PathVariable Long patientId, @PathVariable String encounterId) {
        try {
            Map<String, Object> data = fhirService.list("vitals", patientId, 0, 100, "Encounter/" + encounterId);
            return ResponseEntity.ok(ApiResponse.ok("Vitals retrieved", data));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to list vitals for patient {} encounter {}", patientId, encounterId, e);
            return ResponseEntity.ok(ApiResponse.ok("Vitals retrieved", emptyPage(0, 100)));
        }
    }

    @PostMapping("/api/vitals/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createVitals(
            @PathVariable Long patientId, @PathVariable String encounterId,
            @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> created = fhirService.create("vitals", patientId, body, "Encounter/" + encounterId);
            return ResponseEntity.ok(ApiResponse.ok("Vitals saved", created));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create vitals for patient {} encounter {}", patientId, encounterId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to save vitals: " + e.getMessage()));
        }
    }

    @PutMapping("/api/vitals/{patientId}/{encounterId}/{resourceId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateVitals(
            @PathVariable Long patientId, @PathVariable String encounterId,
            @PathVariable String resourceId, @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> updated = fhirService.update("vitals", patientId, resourceId, body);
            return ResponseEntity.ok(ApiResponse.ok("Vitals updated", updated));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update vitals {} for patient {}", resourceId, patientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update vitals: " + e.getMessage()));
        }
    }

    @DeleteMapping("/api/vitals/{patientId}/{encounterId}/{resourceId}")
    public ResponseEntity<ApiResponse<Void>> deleteVitals(
            @PathVariable Long patientId, @PathVariable String encounterId,
            @PathVariable String resourceId) {
        try {
            fhirService.delete("vitals", resourceId);
            return ResponseEntity.ok(ApiResponse.ok("Vitals deleted", null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete vitals {} for patient {}", resourceId, patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to delete vitals: " + e.getMessage()));
        }
    }

    // ==================== ALLERGY INTOLERANCES (tab: allergies) ====================

    @GetMapping("/api/allergy-intolerances/{patientId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listAllergies(@PathVariable Long patientId) {
        try {
            Map<String, Object> data = fhirService.list("allergies", patientId, 0, 100);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("allergiesList", content);
            return ResponseEntity.ok(ApiResponse.ok("Allergies retrieved", result));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to list allergies for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.ok("Allergies retrieved", Map.of("allergiesList", List.of())));
        }
    }

    // ==================== MEDICAL PROBLEMS (tab: medicalproblems) ====================

    @GetMapping("/api/medical-problems/{patientId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listMedicalProblems(@PathVariable Long patientId) {
        try {
            Map<String, Object> data = fhirService.list("medicalproblems", patientId, 0, 100);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("problemsList", content);
            return ResponseEntity.ok(ApiResponse.ok("Medical problems retrieved", result));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to list medical problems for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.ok("Medical problems retrieved", Map.of("problemsList", List.of())));
        }
    }

    // ==================== PATIENT SUB-RESOURCES ====================

    @GetMapping("/api/patients/{patientId}/appointments")
    public ResponseEntity<ApiResponse<Map<String, Object>>> patientAppointments(
            @PathVariable Long patientId, @RequestParam(defaultValue = "5") int limit) {
        try {
            Map<String, Object> data = fhirService.list("appointments", patientId, 0, limit);
            transformAppointmentList(data, null);
            return ResponseEntity.ok(ApiResponse.ok("Patient appointments", data));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to list appointments for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.ok("Patient appointments", emptyPage(0, limit)));
        }
    }

    @GetMapping("/api/patients/{patientId}/medications")
    public ResponseEntity<ApiResponse<Map<String, Object>>> patientMedications(
            @PathVariable Long patientId, @RequestParam(defaultValue = "5") int limit) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Patient medications", fhirService.list("medications", patientId, 0, limit)));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to list medications for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.ok("Patient medications", emptyPage(0, limit)));
        }
    }

    @GetMapping("/api/patients/{patientId}/labs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> patientLabs(
            @PathVariable Long patientId, @RequestParam(defaultValue = "5") int limit) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Patient labs", fhirService.list("labs", patientId, 0, limit)));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to list labs for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.ok("Patient labs", emptyPage(0, limit)));
        }
    }

    // ==================== ENCOUNTER HELPERS ====================

    private ResponseEntity<ApiResponse<Map<String, Object>>> updateEncounterStatus(
            String id, Encounter.EncounterStatus newStatus, String message) {
        org.hl7.fhir.r4.model.Resource raw = fhirService.readRawResource("Encounter", id);
        if (raw instanceof Encounter encounter) {
            encounter.setStatus(newStatus);
            fhirService.updateRawResource(encounter);
            Map<String, Object> data = fhirService.get("encounters", null, id);
            return ResponseEntity.ok(ApiResponse.ok(message, enrichEncounterFields(data != null ? data : Map.of())));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("Encounter not found: " + id));
    }

    private void checkEncounterNotSigned(String encounterId) {
        try {
            org.hl7.fhir.r4.model.Resource raw = fhirService.readRawResource("Encounter", encounterId);
            if (raw instanceof Encounter encounter && encounter.getStatus() == Encounter.EncounterStatus.FINISHED) {
                throw new IllegalStateException("Cannot modify data for a signed encounter. Please unsign the encounter first.");
            }
        } catch (IllegalStateException | AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            // If we can't read, let the operation proceed
            log.debug("Could not check encounter status for {}: {}", encounterId, e.getMessage());
        }
    }

    private static final Map<String, String> ENCOUNTER_TYPE_LABELS = Map.of(
            "AMB", "Ambulatory",
            "EMER", "Emergency",
            "IMP", "Inpatient",
            "OBSENC", "Observation",
            "HH", "Home Health",
            "VR", "Virtual"
    );

    private static Map<String, Object> enrichEncounterFields(Map<String, Object> data) {
        // Map FHIR status names to app-level status
        Object rawStatus = data.get("status");
        if (rawStatus instanceof String s) {
            data.put("status", switch (s.toLowerCase()) {
                case "finished" -> "SIGNED";
                case "in-progress", "inprogress", "arrived", "planned" -> "UNSIGNED";
                default -> "INCOMPLETE";
            });
        }

        // Alias startDate → encounterDate (frontend expects this field name)
        if (!data.containsKey("encounterDate") && data.containsKey("startDate")) {
            data.put("encounterDate", data.get("startDate"));
        }

        // Map type code → visitCategory label (e.g., "AMB" → "Ambulatory")
        if (!data.containsKey("visitCategory")) {
            Object type = data.get("type");
            if (type instanceof String code) {
                data.put("visitCategory", ENCOUNTER_TYPE_LABELS.getOrDefault(code, code));
            }
        }

        // Extract patientId from patientRef (e.g., "Patient/12345" → 12345)
        Object patientRef = data.get("patientRef");
        if (patientRef instanceof String ref && !data.containsKey("patientId")) {
            String idPart = ref.contains("/") ? ref.substring(ref.lastIndexOf('/') + 1) : ref;
            try {
                data.put("patientId", Long.parseLong(idPart));
            } catch (NumberFormatException ignored) {
                data.put("patientId", idPart);
            }
        }

        // Resolve display names for FHIR references
        // resolveReferences() sets {key}Display fields (e.g., providerDisplay, patientRefDisplay)
        Object providerDisplay = data.get("providerDisplay");
        if (providerDisplay instanceof String name && !name.isBlank()) {
            data.put("encounterProvider", name);
        }
        Object patientDisplay = data.get("patientRefDisplay");
        if (patientDisplay instanceof String name && !name.isBlank()) {
            data.put("patientName", name);
        }

        return data;
    }

    // ==================== SHAPE TRANSFORMATION HELPERS ====================

    /**
     * Convert flat dot-separated keys to nested objects.
     * e.g. {"identification.firstName": "David"} → {"identification": {"firstName": "David"}}
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> flatToNested(Map<String, Object> flat) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : flat.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                // Already nested - just copy
                result.put(key, value);
                continue;
            }
            String[] parts = key.split("\\.");
            if (parts.length == 1) {
                result.put(key, value);
            } else {
                Map<String, Object> current = result;
                for (int i = 0; i < parts.length - 1; i++) {
                    Object existing = current.get(parts[i]);
                    if (existing instanceof Map) {
                        current = (Map<String, Object>) existing;
                    } else {
                        Map<String, Object> next = new LinkedHashMap<>();
                        current.put(parts[i], next);
                        current = next;
                    }
                }
                current.put(parts[parts.length - 1], value);
            }
        }
        return result;
    }

    /**
     * Convert nested objects to flat dot-separated keys.
     * e.g. {"identification": {"firstName": "David"}} → {"identification.firstName": "David"}
     */
    static Map<String, Object> nestedToFlat(Map<String, Object> nested) {
        Map<String, Object> result = new LinkedHashMap<>();
        flattenMap("", nested, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void flattenMap(String prefix, Map<String, Object> map, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map && !((Map<?, ?>) value).isEmpty()) {
                flattenMap(key, (Map<String, Object>) value, result);
            } else {
                result.put(key, value);
            }
        }
    }

    /**
     * Basic provider field enrichment (static — safe for list views and portal).
     * Builds computed name and default systemAccess.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> basicEnrichProviderFields(Map<String, Object> provider) {
        Map<String, Object> identification = (Map<String, Object>) provider.get("identification");
        if (identification != null) {
            String firstName = String.valueOf(identification.getOrDefault("firstName", ""));
            String lastName = String.valueOf(identification.getOrDefault("lastName", ""));
            provider.put("name", (firstName + " " + lastName).trim());
            // Pull NPI from identification block to top-level for easy access
            Object npiObj = identification.get("npi");
            if (npiObj != null && !String.valueOf(npiObj).isBlank() && !"null".equals(String.valueOf(npiObj))) {
                provider.putIfAbsent("npi", String.valueOf(npiObj));
            }
        }
        Map<String, Object> systemAccess = (Map<String, Object>) provider.get("systemAccess");
        if (systemAccess == null) {
            systemAccess = new LinkedHashMap<>();
            provider.put("systemAccess", systemAccess);
        }
        if (!systemAccess.containsKey("status")) {
            systemAccess.put("status", "ACTIVE");
        }
        return provider;
    }

    /**
     * Full provider field enrichment (instance — includes Keycloak account lookup).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> enrichProviderFields(Map<String, Object> provider) {
        basicEnrichProviderFields(provider);

        // Look up linked Keycloak user
        Map<String, Object> systemAccess = (Map<String, Object>) provider.get("systemAccess");
        String fhirId = String.valueOf(provider.getOrDefault("fhirId", ""));
        if (!fhirId.isBlank()) {
            try {
                Map<String, Object> kcUser = keycloakUserService.findUserByAttribute("practitioner_fhir_id", fhirId);
                if (kcUser != null) {
                    systemAccess.put("hasAccount", true);
                    systemAccess.put("keycloakUserId", kcUser.get("id"));
                    systemAccess.put("email", kcUser.get("email"));
                    systemAccess.put("accountEnabled", kcUser.getOrDefault("enabled", false));
                    // Extract NPI from Keycloak user attributes and surface at top level
                    @SuppressWarnings("unchecked")
                    Map<String, ?> kcAttrs = (Map<String, ?>) kcUser.get("attributes");
                    if (kcAttrs != null) {
                        Object npiAttr = kcAttrs.get("npi");
                        String npiVal = null;
                        if (npiAttr instanceof List<?> npiList && !npiList.isEmpty()) {
                            npiVal = String.valueOf(npiList.get(0));
                        } else if (npiAttr instanceof String s) {
                            npiVal = s;
                        }
                        if (npiVal != null && !npiVal.isBlank() && !"null".equals(npiVal)) {
                            provider.putIfAbsent("npi", npiVal);
                        }
                    }
                } else {
                    systemAccess.put("hasAccount", false);
                }
            } catch (AccessDeniedException e) {
                throw e;
            } catch (Exception e) {
                log.debug("Could not look up Keycloak user for provider {}", fhirId);
                systemAccess.put("hasAccount", false);
            }
        }
        return provider;
    }

    /**
     * Add computed fields for patient compatibility.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> enrichPatientFields(Map<String, Object> patient) {
        // Build top-level fullName/name from nested identification fields for patient search
        Object identification = patient.get("identification");
        if (identification instanceof Map<?, ?> idMap) {
            Object firstObj = idMap.get("firstName");
            Object lastObj = idMap.get("lastName");
            String first = firstObj != null ? String.valueOf(firstObj).trim() : "";
            String last = lastObj != null ? String.valueOf(lastObj).trim() : "";
            String full = (first + " " + last).trim();
            if (!full.isEmpty()) {
                patient.put("fullName", full);
                patient.put("name", full);
                patient.put("firstName", first);
                patient.put("lastName", last);
            }
        }
        // Fallback: build name from top-level firstName/lastName if identification block was absent
        if (!patient.containsKey("name") || patient.get("name") == null) {
            String first = String.valueOf(patient.getOrDefault("firstName", "")).trim();
            String last = String.valueOf(patient.getOrDefault("lastName", "")).trim();
            String full = (first + " " + last).trim();
            if (!full.isEmpty()) {
                patient.put("name", full);
                if (!patient.containsKey("fullName") || patient.get("fullName") == null) {
                    patient.put("fullName", full);
                }
            }
        }
        // Normalize status: FHIR Patient.active is boolean, map to human-readable label
        Object rawStatus = patient.get("status");
        if (rawStatus == null) {
            patient.put("status", "Active");
        } else {
            String s = String.valueOf(rawStatus);
            if ("true".equalsIgnoreCase(s)) {
                patient.put("status", "Active");
            } else if ("false".equalsIgnoreCase(s)) {
                patient.put("status", "Inactive");
            }
        }
        // Ensure audit object exists
        if (!patient.containsKey("audit")) {
            Map<String, Object> audit = new LinkedHashMap<>();
            String lastUpdated = String.valueOf(patient.getOrDefault("_lastUpdated", ""));
            if (!lastUpdated.isBlank() && !"null".equals(lastUpdated)) {
                audit.put("createdDate", lastUpdated);
                audit.put("lastModifiedDate", lastUpdated);
            } else {
                String now = java.time.Instant.now().toString();
                audit.put("createdDate", now);
                audit.put("lastModifiedDate", now);
            }
            patient.put("audit", audit);
        }
        return patient;
    }

    /**
     * Enrich a single patient record with linked Keycloak account info.
     * Only for single-patient views (not list), to avoid N+1 Keycloak calls.
     */
    @SuppressWarnings("unchecked")
    private void enrichPatientAccountInfo(Map<String, Object> patient) {
        String fhirId = String.valueOf(patient.getOrDefault("fhirId", ""));
        if (fhirId.isBlank()) return;
        try {
            Map<String, Object> kcUser = keycloakUserService.findUserByAttribute("patient_fhir_id", fhirId);
            if (kcUser != null) {
                patient.put("hasAccount", true);
                patient.put("keycloakUserId", kcUser.get("id"));
                patient.put("accountEmail", kcUser.get("email"));
                patient.put("accountEnabled", kcUser.getOrDefault("enabled", false));
            } else {
                patient.put("hasAccount", false);
            }
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Could not look up Keycloak user for patient {}", fhirId);
            patient.put("hasAccount", false);
        }
    }

    // ==================== PRACTICE HELPERS ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> findPractice(String identifier) {
        try {
            Long id = Long.parseLong(identifier);
            return fhirService.get("practice", null, String.valueOf(id));
        } catch (NumberFormatException e) {
            // Search by name
            try {
                Map<String, Object> data = fhirService.searchByName("practice", identifier, 0, 1);
                List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
                return content.isEmpty() ? null : content.get(0);
            } catch (Exception ex) {
                return null;
            }
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findFirstPractice() {
        try {
            Map<String, Object> data = fhirService.listAll("practice", 0, 1);
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            return content.isEmpty() ? null : content.get(0);
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== APPOINTMENT TRANSFORMATION HELPERS ====================

    @SuppressWarnings("unchecked")
    private void transformAppointmentList(Map<String, Object> data, String statusFilter) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
        List<Map<String, Object>> transformed = content.stream()
                .map(this::transformAppointmentFromFhir)
                .filter(a -> statusFilter == null || statusFilter.isEmpty()
                        || "All".equalsIgnoreCase(statusFilter)
                        || statusFilter.equalsIgnoreCase(String.valueOf(a.get("status"))))
                .collect(Collectors.toList());
        data.put("content", transformed);
    }

    private Map<String, Object> transformAppointmentFromFhir(Map<String, Object> fhirData) {
        Map<String, Object> dto = new LinkedHashMap<>(fhirData);
        // Convert FHIR status code to custom display status name
        String rawStatus = dto.get("status") != null ? String.valueOf(dto.get("status")) : "booked";
        // Map legacy 'arrived' to 'checked-in' first
        if ("arrived".equals(rawStatus)) rawStatus = "checked-in";
        dto.put("status", AppointmentEncounterService.fromFhirCode(rawStatus));
        Object start = fhirData.get("start");
        if (start instanceof String s && s.length() >= 10) {
            dto.put("appointmentStartDate", s.substring(0, 10));
            dto.put("appointmentStartTime", s.length() > 11 ? s.substring(11, Math.min(16, s.length())) : "00:00");
        }
        Object end = fhirData.get("end");
        if (end instanceof String s && s.length() >= 10) {
            dto.put("appointmentEndDate", s.substring(0, 10));
            dto.put("appointmentEndTime", s.length() > 11 ? s.substring(11, Math.min(16, s.length())) : "00:00");
        }
        if (fhirData.containsKey("appointmentType")) {
            Object apptType = fhirData.get("appointmentType");
            dto.put("visitType", extractCodeableConceptText(apptType));
        }
        Object patient = fhirData.get("patient");
        if (patient instanceof String ref) dto.put("patientId", extractIdFromReference(ref));
        Object provider = fhirData.get("provider");
        if (provider instanceof String ref) {
            dto.put("providerId", extractIdFromReference(ref));
            // Carry provider display name if available from FHIR reference resolution
            Object providerDisplay = fhirData.get("providerDisplay");
            if (providerDisplay instanceof String name && !name.isBlank()) {
                dto.put("providerName", name);
            }
        }
        Object location = fhirData.get("location");
        if (location instanceof String ref) dto.put("locationId", extractIdFromReference(ref));
        // Carry patient name if available
        Object patientName = fhirData.get("patientDisplay");
        if (patientName instanceof String name && !name.isBlank()) {
            dto.put("patientName", name);
        }
        return dto;
    }

    private Map<String, Object> transformAppointmentToFhir(Map<String, Object> dto) {
        Map<String, Object> formData = new LinkedHashMap<>();
        copyIfPresent(dto, formData, "status");
        if (!formData.containsKey("status") || formData.get("status") == null) formData.put("status", "Scheduled");
        copyIfPresent(dto, formData, "priority");
        copyIfPresent(dto, formData, "reason");
        copyIfPresent(dto, formData, "description");
        copyIfPresent(dto, formData, "comment");
        copyIfPresent(dto, formData, "room");
        Object startIso = dto.get("start");
        if (startIso instanceof String s && !s.isEmpty()) {
            formData.put("start", s);
        } else {
            String date = (String) dto.get("appointmentStartDate");
            String time = (String) dto.get("appointmentStartTime");
            if (date != null && !date.isEmpty()) formData.put("start", date + "T" + (time != null && !time.isEmpty() ? time : "00:00") + ":00");
        }
        Object endIso = dto.get("end");
        if (endIso instanceof String s && !s.isEmpty()) {
            formData.put("end", s);
        } else {
            String date = (String) dto.get("appointmentEndDate");
            String time = (String) dto.get("appointmentEndTime");
            if (date != null && !date.isEmpty()) formData.put("end", date + "T" + (time != null && !time.isEmpty() ? time : "00:00") + ":00");
        }
        if (dto.containsKey("visitType")) formData.put("appointmentType", dto.get("visitType"));
        if (dto.get("patientId") != null) formData.put("patient", "Patient/" + dto.get("patientId"));
        if (dto.get("providerId") != null) formData.put("provider", "Practitioner/" + dto.get("providerId"));
        if (dto.get("locationId") != null) formData.put("location", "Location/" + dto.get("locationId"));
        return formData;
    }

    @SuppressWarnings("unchecked")
    private String extractCodeableConceptText(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        if (value instanceof Map<?, ?> map) {
            Object text = map.get("text");
            if (text instanceof String t && !t.isBlank()) return t;
            Object coding = map.get("coding");
            if (coding instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Map<?, ?> codeMap) {
                    Object display = codeMap.get("display");
                    if (display instanceof String d && !d.isBlank()) return d;
                    Object code = codeMap.get("code");
                    if (code instanceof String c) return c;
                }
            }
        }
        return String.valueOf(value);
    }

    private void copyIfPresent(Map<String, Object> from, Map<String, Object> to, String key) {
        Object value = from.get(key);
        if (value != null) to.put(key, value);
    }

    private Long extractIdFromReference(String reference) {
        if (reference == null) return null;
        try {
            String id = reference.contains("/") ? reference.substring(reference.lastIndexOf('/') + 1) : reference;
            return Long.parseLong(id);
        } catch (NumberFormatException e) { return null; }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(value)); } catch (NumberFormatException e) { return null; }
    }

    private Map<String, Object> emptyPage(int page, int size) {
        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("content", List.of());
        empty.put("page", page);
        empty.put("size", size);
        empty.put("totalElements", 0);
        empty.put("totalPages", 0);
        empty.put("hasNext", false);
        return empty;
    }
}
