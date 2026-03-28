package org.ciyex.ehr.fhir;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.notification.service.AppointmentNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic FHIR resource controller — replaces per-resource controllers.
 * Uses tab_field_config to dynamically handle CRUD for any FHIR resource type.
 * Per-resource-type scope enforcement is handled by FhirScopeEnforcer in the service layer.
 */
@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/fhir-resource")
@RequiredArgsConstructor
@Slf4j
public class GenericFhirResourceController {

    private final GenericFhirResourceService resourceService;
    private final AppointmentNotificationService appointmentNotificationService;

    @GetMapping("/{tabKey}/patient/{patientId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @PathVariable String tabKey,
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String encounterRef) {
        try {
            Map<String, Object> data = resourceService.list(tabKey, patientId, page, size, encounterRef);
            return ResponseEntity.ok(ApiResponse.ok("Resources retrieved", data));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("No tab config found for tab '{}': {}", tabKey, e.getMessage());
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("content", List.of());
            empty.put("page", page);
            empty.put("size", size);
            empty.put("totalElements", 0);
            empty.put("totalPages", 0);
            empty.put("hasNext", false);
            return ResponseEntity.ok(ApiResponse.ok("No configuration found for tab: " + tabKey, empty));
        } catch (Exception e) {
            log.error("Error listing resources for tab '{}' patient {}", tabKey, patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to load records: " + e.getMessage()));
        }
    }

    @GetMapping("/{tabKey}/patient/{patientId}/{resourceId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(
            @PathVariable String tabKey,
            @PathVariable Long patientId,
            @PathVariable String resourceId) {
        try {
            Map<String, Object> data = resourceService.get(tabKey, patientId, resourceId);
            if (data == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Resource not found: " + resourceId));
            }
            return ResponseEntity.ok(ApiResponse.ok("Resource retrieved", data));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting resource for tab '{}' patient {} id {}", tabKey, patientId, resourceId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to load record: " + e.getMessage()));
        }
    }

    @PostMapping("/{tabKey}/patient/{patientId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            @PathVariable String tabKey,
            @PathVariable Long patientId,
            @RequestParam(required = false) String encounterRef,
            @RequestBody Map<String, Object> formData) {
        try {
            // Check for dry-run flag in request body
            boolean dryRun = "true".equals(String.valueOf(formData.remove("_dryRun")));
            if (dryRun) {
                Map<String, Object> result = resourceService.dryRun(tabKey, patientId, formData);
                return ResponseEntity.ok(ApiResponse.ok("Dry run completed", result));
            }
            Map<String, Object> created = resourceService.create(tabKey, patientId, formData, encounterRef);
            fireAppointmentNotificationIfNeeded(tabKey, formData, created);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Resource created", created));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating resource for tab '{}' patient {}", tabKey, patientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create record: " + e.getMessage()));
        }
    }

    @PutMapping("/{tabKey}/patient/{patientId}/{resourceId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(
            @PathVariable String tabKey,
            @PathVariable Long patientId,
            @PathVariable String resourceId,
            @RequestBody Map<String, Object> formData) {
        try {
            Map<String, Object> updated = resourceService.update(tabKey, patientId, resourceId, formData);
            return ResponseEntity.ok(ApiResponse.ok("Resource updated", updated));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Resource not found for tab '{}' patient {} id {}: {}", tabKey, patientId, resourceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating resource for tab '{}' patient {} id {}", tabKey, patientId, resourceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update record. Please try again."));
        }
    }

    @DeleteMapping("/{tabKey}/patient/{patientId}/{resourceId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String tabKey,
            @PathVariable Long patientId,
            @PathVariable String resourceId) {
        try {
            resourceService.delete(tabKey, resourceId);
            return ResponseEntity.ok(ApiResponse.ok("Resource deleted", null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Resource not found for deletion: tab={}, resourceId={}", tabKey, resourceId);
            return ResponseEntity.ok(ApiResponse.ok("Resource already deleted", null));
        } catch (Exception e) {
            log.error("Error deleting resource for tab '{}' resourceId {}", tabKey, resourceId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to delete: " + e.getMessage()));
        }
    }

    // ==================== Non-patient-scoped endpoints (Settings pages) ====================

    @GetMapping("/{tabKey}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listAll(
            @PathVariable String tabKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        try {
            Map<String, Object> data = (search != null && !search.isBlank())
                    ? resourceService.searchByName(tabKey, search, page, size)
                    : resourceService.listAll(tabKey, page, size);
            return ResponseEntity.ok(ApiResponse.ok("Resources retrieved", data));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("No tab config found for tab '{}': {}", tabKey, e.getMessage());
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("content", List.of());
            empty.put("page", page);
            empty.put("size", size);
            empty.put("totalElements", 0);
            empty.put("totalPages", 0);
            empty.put("hasNext", false);
            return ResponseEntity.ok(ApiResponse.ok("No configuration found for tab: " + tabKey, empty));
        } catch (Exception e) {
            log.error("Error listing resources for tab '{}'", tabKey, e);
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("content", List.of());
            empty.put("page", page);
            empty.put("size", size);
            empty.put("totalElements", 0);
            empty.put("totalPages", 0);
            empty.put("hasNext", false);
            return ResponseEntity.ok(ApiResponse.ok("Resources retrieved", empty));
        }
    }

    @GetMapping("/{tabKey}/{resourceId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getResource(
            @PathVariable String tabKey,
            @PathVariable String resourceId) {
        try {
            Map<String, Object> data = resourceService.get(tabKey, null, resourceId);
            if (data == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Resource not found: " + resourceId));
            }
            return ResponseEntity.ok(ApiResponse.ok("Resource retrieved", data));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting resource for tab '{}' id {}", tabKey, resourceId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to load record: " + e.getMessage()));
        }
    }

    @PostMapping("/{tabKey}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createResource(
            @PathVariable String tabKey,
            @RequestBody Map<String, Object> formData) {
        try {
            Map<String, Object> created = resourceService.create(tabKey, null, formData);
            fireAppointmentNotificationIfNeeded(tabKey, formData, created);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Resource created", created));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating resource for tab '{}'", tabKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create record: " + e.getMessage()));
        }
    }

    @PutMapping("/{tabKey}/{resourceId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateResource(
            @PathVariable String tabKey,
            @PathVariable String resourceId,
            @RequestBody Map<String, Object> formData) {
        try {
            Map<String, Object> updated = resourceService.update(tabKey, null, resourceId, formData);
            return ResponseEntity.ok(ApiResponse.ok("Resource updated", updated));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating resource for tab '{}' id {}", tabKey, resourceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update record: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{tabKey}/{resourceId}")
    public ResponseEntity<ApiResponse<Void>> deleteResource(
            @PathVariable String tabKey,
            @PathVariable String resourceId) {
        try {
            resourceService.delete(tabKey, resourceId);
            return ResponseEntity.ok(ApiResponse.ok("Resource deleted", null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Resource not found for deletion: tab={}, resourceId={}", tabKey, resourceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Resource not found: " + resourceId));
        } catch (Exception e) {
            log.error("Error deleting resource for tab '{}' resourceId {}", tabKey, resourceId, e);
            String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            if (msg.contains("reference") || msg.contains("Conflict")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error("Cannot delete: this record is referenced by other records"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete: " + msg));
        }
    }

    /**
     * Fire appointment confirmation notification when an appointment is created
     * via the generic FHIR resource endpoint. Resolves patient email before
     * handing off to async notification service.
     */
    private void fireAppointmentNotificationIfNeeded(String tabKey,
                                                      Map<String, Object> formData,
                                                      Map<String, Object> created) {
        if (!"appointments".equals(tabKey)) return;
        try {
            String orgAlias = RequestContext.get().getOrgName();
            // Extract patient ID from form data BEFORE merging (created may overwrite)
            Long patientId = toLong(formData.get("patientId"));
            if (patientId == null) {
                Object patientRef = formData.get("patient");
                if (patientRef instanceof String ref && ref.contains("/")) {
                    patientId = toLong(ref.substring(ref.lastIndexOf("/") + 1));
                }
            }
            if (patientId == null) patientId = toLong(created.get("patientId"));
            log.info("Appointment notification: patientId={}, formKeys={}, createdKeys={}",
                    patientId, formData.keySet(), created.keySet());

            Map<String, Object> notifData = new HashMap<>(formData);
            notifData.putAll(created);
            if (patientId != null) notifData.put("patientId", patientId);
            if (patientId != null && !notifData.containsKey("patientEmail")) {
                try {
                    Map<String, Object> pd = resourceService.get("demographics", patientId, String.valueOf(patientId));
                    if (pd != null) {
                        Object email = pd.get("email");
                        if (email != null && !String.valueOf(email).isBlank()) {
                            notifData.put("patientEmail", String.valueOf(email));
                        }
                        if (!notifData.containsKey("patientEmail") && pd.get("telecom") instanceof List<?> telecoms) {
                            for (Object t : telecoms) {
                                if (t instanceof Map<?, ?> tm && "email".equals(tm.get("system"))) {
                                    notifData.put("patientEmail", String.valueOf(tm.get("value")));
                                    break;
                                }
                            }
                        }
                        String fn = pd.get("firstName") != null ? String.valueOf(pd.get("firstName")) : "";
                        String ln = pd.get("lastName") != null ? String.valueOf(pd.get("lastName")) : "";
                        String name = (fn + " " + ln).trim();
                        if (!name.isBlank()) notifData.putIfAbsent("patientName", name);
                    }
                } catch (Exception e) {
                    log.debug("Could not look up patient {} for notification: {}", patientId, e.getMessage());
                }
            }
            appointmentNotificationService.onAppointmentCreated(orgAlias, notifData);
        } catch (Exception e) {
            log.warn("Failed to trigger appointment notification: {}", e.getMessage());
        }
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(val)); } catch (Exception e) { return null; }
    }
}
