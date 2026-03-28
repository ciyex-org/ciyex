package org.ciyex.ehr.controller.portal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.portal.ApiResponse;
import org.ciyex.ehr.education.service.EducationMaterialService;
import org.ciyex.ehr.education.service.PatientEducationService;
import org.ciyex.ehr.fhir.GenericFhirResourceService;
import org.ciyex.ehr.service.portal.PortalGenericResourceService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import org.ciyex.ehr.controller.FhirFacadeController;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Portal "my data" endpoints — patient-scoped data for the logged-in portal user.
 *
 * Resolves the portal user's email from JWT → FHIR Patient ID via PortalGenericResourceService,
 * then delegates to GenericFhirResourceService for the actual data.
 *
 * Endpoints:
 *   GET  /api/portal/providers        — list all providers for this org (no patient scope)
 *   GET  /api/portal/locations        — list all locations for this org (no patient scope)
 *   GET  /api/fhir/medications/my     — medications for the logged-in patient
 *   GET  /api/portal/vitals/my        — vitals for the logged-in patient
 *   GET  /api/fhir/portal/billing/my  — billing invoices for the logged-in patient
 *   GET  /api/fhir/insurance/my       — insurance coverage for the logged-in patient
 *   GET  /api/portal/appointments     — appointments for the logged-in patient
 *   POST /api/portal/appointments     — create appointment for the logged-in patient
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(
    origins = { "http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001" },
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS },
    allowCredentials = "true"
)
public class PortalMyDataController {

    private final PortalGenericResourceService portalResourceService;
    private final GenericFhirResourceService fhirResourceService;
    private final EducationMaterialService educationMaterialService;
    private final PatientEducationService patientEducationService;

    // ==================== ORG-LEVEL (no patient auth required) ====================

    @SuppressWarnings("unchecked")
    @GetMapping("/api/portal/providers")
    public ApiResponse<Object> getProviders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        try {
            Map<String, Object> data = fhirResourceService.listAll("providers", page, size);
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.getOrDefault("content", List.of());
            // Apply same flatToNested + enrichment as FhirFacadeController so portal gets
            // nested objects (identification.firstName etc.) and computed "name" field.
            List<Map<String, Object>> nested = content.stream()
                    .map(FhirFacadeController::flatToNested)
                    .map(FhirFacadeController::basicEnrichProviderFields)
                    .toList();
            return ApiResponse.success("Providers retrieved", nested);
        } catch (Exception e) {
            log.error("Failed to list providers for portal", e);
            return ApiResponse.error("Failed to retrieve providers");
        }
    }

    /**
     * Returns only providers the logged-in patient has had appointments with (care team).
     * Used by portal messaging to restrict which providers a patient can message.
     */
    @SuppressWarnings("unchecked")
    @GetMapping("/api/portal/my-providers")
    public ApiResponse<Object> getMyProviders(Authentication authentication) {
        String email = extractEmail(authentication);
        if (email == null) return ApiResponse.error("Invalid token: email not found");
        try {
            Long patientId = resolvePatient(email, extractJwt(authentication));

            // Get the patient's appointments to find which providers they've seen
            Map<String, Object> apptData = fhirResourceService.list("appointments", patientId, 0, 200);
            List<Map<String, Object>> appointments = (List<Map<String, Object>>) apptData.getOrDefault("content", List.of());

            // Extract unique provider FHIR IDs from appointment data
            Set<String> providerIds = new LinkedHashSet<>();
            for (Map<String, Object> appt : appointments) {
                // provider field may be "Practitioner/123" or just "123"
                Object provRef = appt.get("provider");
                if (provRef != null) {
                    String ref = String.valueOf(provRef);
                    providerIds.add(ref.startsWith("Practitioner/") ? ref.substring("Practitioner/".length()) : ref);
                }
                // Also check providerId field (some data shapes)
                Object provId = appt.get("providerId");
                if (provId != null) {
                    String ref = String.valueOf(provId);
                    providerIds.add(ref.startsWith("Practitioner/") ? ref.substring("Practitioner/".length()) : ref);
                }
            }

            if (providerIds.isEmpty()) {
                return ApiResponse.success("Care team providers retrieved", List.of());
            }

            // Get all providers and filter to only those in the patient's appointment history
            Map<String, Object> allProviders = fhirResourceService.listAll("providers", 0, 200);
            List<Map<String, Object>> content = (List<Map<String, Object>>) allProviders.getOrDefault("content", List.of());
            List<Map<String, Object>> careTeam = content.stream()
                    .filter(p -> {
                        Object id = p.get("id");
                        return id != null && providerIds.contains(String.valueOf(id));
                    })
                    .map(FhirFacadeController::flatToNested)
                    .map(FhirFacadeController::basicEnrichProviderFields)
                    .toList();

            return ApiResponse.success("Care team providers retrieved", careTeam);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Patient not linked — return empty list
            return ApiResponse.success("Care team providers retrieved", List.of());
        } catch (Exception e) {
            log.error("Failed to fetch care team providers for portal user {}", email, e);
            return ApiResponse.error("Failed to retrieve care team providers");
        }
    }

    @GetMapping("/api/portal/locations")
    public ApiResponse<Object> getLocations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        try {
            Map<String, Object> data = fhirResourceService.listAll("facilities", page, size);
            return ApiResponse.success("Locations retrieved", data.get("content"));
        } catch (Exception e) {
            log.error("Failed to list locations for portal", e);
            return ApiResponse.error("Failed to retrieve locations");
        }
    }

    // ==================== PATIENT-SCOPED "MY" ENDPOINTS ====================

    @GetMapping("/api/fhir/medications/my")
    public ApiResponse<Object> getMyMedications(Authentication authentication) {
        return fetchMyData("medications", authentication, "medications");
    }

    @GetMapping("/api/portal/vitals/my")
    public ApiResponse<Object> getMyVitals(Authentication authentication) {
        return fetchMyData("vitals", authentication, "vitals");
    }

    @GetMapping("/api/fhir/vitals/my")
    public ApiResponse<Object> getMyVitalsFhir(Authentication authentication) {
        return fetchMyData("vitals", authentication, "vitals");
    }

    @GetMapping("/api/fhir/portal/billing/my")
    public ApiResponse<Object> getMyBilling(Authentication authentication) {
        return fetchMyData("billing", authentication, "billing");
    }

    @GetMapping("/api/fhir/insurance/my")
    public ApiResponse<Object> getMyInsurance(Authentication authentication) {
        return fetchMyData("insurance-coverage", authentication, "insurance");
    }

    @GetMapping("/api/portal/communications/my")
    public ApiResponse<Object> getMyCommunications(Authentication authentication) {
        return fetchMyData("messaging", authentication, "communications");
    }

    @GetMapping("/api/portal/allergies")
    public ApiResponse<Object> getMyAllergies(Authentication authentication) {
        return fetchMyData("allergies", authentication, "allergies");
    }

    @GetMapping("/api/portal/history")
    public ApiResponse<Object> getMyHistory(Authentication authentication) {
        return fetchMyData("history", authentication, "history");
    }

    @GetMapping("/api/portal/lab-orders")
    public ApiResponse<Object> getMyLabOrders(Authentication authentication) {
        return fetchMyData("labs", authentication, "lab-orders");
    }

    /** Reports — return lab results / diagnostic reports for the logged-in patient */
    @GetMapping("/api/fhir/portal/reports/my")
    public ApiResponse<Object> getMyReports(Authentication authentication) {
        String email = extractEmail(authentication);
        if (email == null) return ApiResponse.error("Invalid token: email not found");
        try {
            Long patientId = resolvePatient(email, extractJwt(authentication));
            Map<String, Object> data = fhirResourceService.list("labs", patientId, 0, 100);
            Object content = data.get("content");
            return ApiResponse.success("Reports retrieved", content != null ? content : List.of());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.success("Reports retrieved", List.of());
        } catch (Exception e) {
            log.error("Failed to fetch reports for portal user {}", email, e);
            return ApiResponse.success("Reports retrieved", List.of());
        }
    }

    /** Education assignments for the logged-in patient. */
    @GetMapping("/api/portal/patient-education-assignments/my-assignments")
    public ApiResponse<Object> getMyEducationAssignments(Authentication authentication) {
        String email = extractEmail(authentication);
        if (email == null) return ApiResponse.error("Invalid token: email not found");
        try {
            Long patientId = resolvePatient(email, extractJwt(authentication));
            var assignments = patientEducationService.getByPatient(patientId);
            return ApiResponse.success("Education assignments retrieved", assignments);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Patient not linked — return empty list gracefully
            return ApiResponse.success("Education assignments retrieved", List.of());
        } catch (Exception e) {
            log.error("Failed to fetch education assignments for portal user {}", email, e);
            return ApiResponse.success("Education assignments retrieved", List.of());
        }
    }

    /** General patient education topics — paginated list of all active materials. */
    @GetMapping("/api/patient-education")
    public ApiResponse<Object> getPatientEducation(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        try {
            var materials = educationMaterialService.getAll(PageRequest.of(page, size));
            return ApiResponse.success("Education topics retrieved",
                    Map.of("content", materials.getContent(), "totalElements", materials.getTotalElements()));
        } catch (Exception e) {
            log.error("Failed to fetch education materials", e);
            return ApiResponse.success("Education topics retrieved", Map.of("content", List.of(), "totalElements", 0));
        }
    }

    @GetMapping("/api/portal/appointments")
    public ApiResponse<Object> getMyAppointments(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        String email = extractEmail(authentication);
        if (email == null) return ApiResponse.error("Invalid token: email not found");
        try {
            Long patientId = resolvePatient(email, extractJwt(authentication));
            Map<String, Object> data = fhirResourceService.list("appointments", patientId, page, size);
            return ApiResponse.success("Appointments retrieved", data);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error("Portal account not linked. Please contact your provider.");
        } catch (Exception e) {
            log.error("Failed to fetch appointments for portal user {}", email, e);
            return ApiResponse.error("Failed to retrieve appointments");
        }
    }

    @PostMapping("/api/portal/appointments")
    public ApiResponse<Object> createAppointment(
            Authentication authentication,
            @RequestBody Map<String, Object> formData) {
        String email = extractEmail(authentication);
        if (email == null) return ApiResponse.error("Invalid token: email not found");
        try {
            Long patientId = resolvePatient(email, extractJwt(authentication));
            // Transform portal form data to FHIR-compatible format
            Map<String, Object> fhirData = transformAppointmentData(formData, patientId);
            Map<String, Object> created = fhirResourceService.create("appointments", patientId, fhirData);
            return ApiResponse.success("Appointment created", created);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error("Portal account not linked. Please contact your provider.");
        } catch (Exception e) {
            log.error("Failed to create appointment for portal user {}", email, e);
            return ApiResponse.error("Failed to create appointment: " + e.getMessage());
        }
    }

    /**
     * Transform portal appointment form data to FHIR-compatible fields.
     * Maps visitType → appointmentType, converts priority string → integer,
     * ensures start/end are ISO format, builds proper participant references.
     */
    private Map<String, Object> transformAppointmentData(Map<String, Object> dto, Long patientId) {
        Map<String, Object> formData = new LinkedHashMap<>();

        // Status: default to "booked"
        Object status = dto.get("status");
        formData.put("status", status != null && !String.valueOf(status).isBlank()
                ? String.valueOf(status) : "booked");

        // Priority: convert string name → FHIR unsignedInt (0=unspecified, 2=urgent, 5=routine, 9=low)
        Object priority = dto.get("priority");
        if (priority != null) {
            String pStr = String.valueOf(priority).toLowerCase().trim();
            try {
                formData.put("priority", Integer.parseInt(pStr));
            } catch (NumberFormatException e) {
                int pInt = switch (pStr) {
                    case "emergency", "stat" -> 1;
                    case "urgent" -> 2;
                    case "routine", "normal" -> 5;
                    case "low" -> 9;
                    default -> 5;
                };
                formData.put("priority", pInt);
            }
        }

        // Reason
        if (dto.containsKey("reason")) formData.put("reason", dto.get("reason"));
        if (dto.containsKey("description")) formData.put("description", dto.get("description"));

        // Start/end: prefer ISO format, fall back to date+time composition
        Object startIso = dto.get("start");
        if (startIso instanceof String s && !s.isEmpty()) {
            formData.put("start", s);
        } else {
            String date = dto.get("appointmentStartDate") != null ? String.valueOf(dto.get("appointmentStartDate")) : null;
            String time = dto.get("appointmentStartTime") != null ? String.valueOf(dto.get("appointmentStartTime")) : null;
            if (date != null && !date.isEmpty()) {
                formData.put("start", date + "T" + (time != null && !time.isEmpty() ? time : "00:00") + ":00");
            }
        }
        Object endIso = dto.get("end");
        if (endIso instanceof String s && !s.isEmpty()) {
            formData.put("end", s);
        } else {
            String date = dto.get("appointmentEndDate") != null ? String.valueOf(dto.get("appointmentEndDate")) : null;
            String time = dto.get("appointmentEndTime") != null ? String.valueOf(dto.get("appointmentEndTime")) : null;
            if (date != null && !date.isEmpty()) {
                formData.put("end", date + "T" + (time != null && !time.isEmpty() ? time : "00:00") + ":00");
            }
        }

        // Appointment type
        if (dto.containsKey("visitType")) {
            formData.put("appointmentType", dto.get("visitType"));
        } else if (dto.containsKey("appointmentType")) {
            formData.put("appointmentType", dto.get("appointmentType"));
        }

        // Participant references: build from IDs
        formData.put("patient", "Patient/" + patientId);
        if (dto.get("providerId") != null) {
            formData.put("provider", "Practitioner/" + dto.get("providerId"));
        } else if (dto.get("provider") != null) {
            String prov = String.valueOf(dto.get("provider"));
            formData.put("provider", prov.startsWith("Practitioner/") ? prov : "Practitioner/" + prov);
        }
        if (dto.get("locationId") != null) {
            formData.put("location", "Location/" + dto.get("locationId"));
        } else if (dto.get("location") != null) {
            String loc = String.valueOf(dto.get("location"));
            formData.put("location", loc.startsWith("Location/") ? loc : "Location/" + loc);
        }

        return formData;
    }

    // ==================== PORTAL INSURANCE CRUD ====================
    // Uses "insurance-coverage" tab config (same as GET /api/fhir/insurance/my)
    // so field names are consistent between create, update, and read.

    private static final String INSURANCE_TAB = "insurance-coverage";

    /**
     * Map portal form fields → insurance-coverage tab field keys.
     * This ensures the FHIR Coverage resource is populated with correct paths
     * matching what the GET endpoint returns.
     */
    private Map<String, Object> mapPortalInsuranceFields(Map<String, Object> body) {
        Map<String, Object> mapped = new LinkedHashMap<>();
        // Insurance tier: portal sends "coverageType", tab expects "insuranceType"
        Object tier = body.get("coverageType");
        if (tier != null) mapped.put("insuranceType", tier.toString().toLowerCase());
        // Plan info — same keys
        if (body.containsKey("planName")) mapped.put("planName", body.get("planName"));
        if (body.containsKey("policyNumber")) mapped.put("policyNumber", body.get("policyNumber"));
        if (body.containsKey("groupNumber")) mapped.put("groupNumber", body.get("groupNumber"));
        // Copay — same key
        if (body.containsKey("copayAmount")) mapped.put("copayAmount", body.get("copayAmount"));
        // Dates: portal sends coverageStartDate/coverageEndDate, tab expects policyEffectiveDate/policyEndDate
        if (body.containsKey("coverageStartDate")) mapped.put("policyEffectiveDate", body.get("coverageStartDate"));
        if (body.containsKey("coverageEndDate")) mapped.put("policyEndDate", body.get("coverageEndDate"));
        // Payer name from insurance company
        Object ic = body.get("insuranceCompany");
        if (ic instanceof Map<?, ?> icMap) {
            Object name = icMap.get("name");
            if (name != null) mapped.put("payerName", name);
        }
        if (body.containsKey("payerName")) mapped.put("payerName", body.get("payerName"));
        // Subscriber fields
        if (body.containsKey("byholderRelation")) mapped.put("subscriberRelationship", body.get("byholderRelation"));
        if (body.containsKey("byholderName")) {
            String fullName = String.valueOf(body.get("byholderName"));
            String[] parts = fullName.trim().split("\\s+", 2);
            mapped.put("subscriberFirstName", parts[0]);
            if (parts.length > 1) mapped.put("subscriberLastName", parts[1]);
        }
        if (body.containsKey("subscriberPhone")) mapped.put("subscriberPhone", body.get("subscriberPhone"));
        if (body.containsKey("subscriberEmployer")) mapped.put("subscriberEmployer", body.get("subscriberEmployer"));
        // Subscriber address — combine into single field
        StringBuilder addr = new StringBuilder();
        if (body.containsKey("subscriberAddressLine1")) addr.append(body.get("subscriberAddressLine1"));
        if (body.containsKey("subscriberAddressLine2")) { if (!addr.isEmpty()) addr.append(", "); addr.append(body.get("subscriberAddressLine2")); }
        if (body.containsKey("subscriberCity")) { if (!addr.isEmpty()) addr.append(", "); addr.append(body.get("subscriberCity")); }
        if (body.containsKey("subscriberState")) { if (!addr.isEmpty()) addr.append(" "); addr.append(body.get("subscriberState")); }
        if (body.containsKey("subscriberZipCode")) { if (!addr.isEmpty()) addr.append(" "); addr.append(body.get("subscriberZipCode")); }
        if (!addr.isEmpty()) mapped.put("subscriberAddress", addr.toString());
        // Always set status to active
        mapped.put("status", "active");
        // Preserve raw portal fields in the map so they're stored in the formData extension
        mapped.putAll(body);
        return mapped;
    }

    @PostMapping("/api/portal/insurance")
    public ResponseEntity<ApiResponse<Object>> createMyInsurance(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        String email = extractEmail(authentication);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid token: email not found"));
        try {
            Long patientId = resolvePatient(email, extractJwt(authentication));
            Map<String, Object> mapped = mapPortalInsuranceFields(body);
            Map<String, Object> created = fhirResourceService.create(INSURANCE_TAB, patientId, mapped);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Coverage created", created));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.error("Portal account not linked. Please contact your provider."));
        } catch (Exception e) {
            log.error("Failed to create insurance for portal user {}", email, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to create coverage: " + e.getMessage()));
        }
    }

    @PutMapping("/api/portal/insurance/{fhirId}")
    public ResponseEntity<ApiResponse<Object>> updateMyInsurance(
            Authentication authentication,
            @PathVariable String fhirId,
            @RequestBody Map<String, Object> body) {
        String email = extractEmail(authentication);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid token: email not found"));
        try {
            Long patientId = resolvePatient(email, extractJwt(authentication));
            Map<String, Object> mapped = mapPortalInsuranceFields(body);
            Map<String, Object> updated = fhirResourceService.update(INSURANCE_TAB, patientId, fhirId, mapped);
            return ResponseEntity.ok(ApiResponse.success("Coverage updated", updated));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.error("Portal account not linked. Please contact your provider."));
        } catch (Exception e) {
            log.error("Failed to update insurance {} for portal user {}", fhirId, email, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to update coverage: " + e.getMessage()));
        }
    }

    @DeleteMapping("/api/portal/insurance/{fhirId}")
    public ResponseEntity<ApiResponse<Object>> deleteMyInsurance(
            Authentication authentication,
            @PathVariable String fhirId) {
        String email = extractEmail(authentication);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid token: email not found"));
        try {
            fhirResourceService.delete(INSURANCE_TAB, fhirId);
            return ResponseEntity.ok(ApiResponse.success("Coverage deleted", null));
        } catch (Exception e) {
            log.error("Failed to delete insurance {} for portal user {}", fhirId, email, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to delete coverage: " + e.getMessage()));
        }
    }

    // ==================== HELPERS ====================

    private ApiResponse<Object> fetchMyData(String tabKey, Authentication authentication, String label) {
        String email = extractEmail(authentication);
        if (email == null) return ApiResponse.error("Invalid token: email not found");
        try {
            Long patientId = resolvePatient(email, extractJwt(authentication));
            Map<String, Object> data = fhirResourceService.list(tabKey, patientId, 0, 100);
            // Return the content list directly — portal pages expect data.data as an array
            Object content = data.get("content");
            return ApiResponse.success(label + " retrieved", content != null ? content : List.of());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error("Portal account not linked. Please contact your provider.");
        } catch (Exception e) {
            log.error("Failed to fetch {} for portal user {}", label, email, e);
            return ApiResponse.error("Failed to retrieve " + label);
        }
    }

    private Long resolvePatient(String email) {
        return Long.parseLong(portalResourceService.resolvePatientId(email));
    }

    private Long resolvePatient(String email, Jwt jwt) {
        return Long.parseLong(portalResourceService.resolvePatientId(email, jwt));
    }

    private String extractEmail(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("email");
        }
        return null;
    }

    private Jwt extractJwt(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }
}
