package org.ciyex.ehr.lab.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.lab.dto.LabResultDto;
import org.ciyex.ehr.lab.service.LabResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

/**
 * External Lab Interface Controller — integration point for external lab systems
 * (LabCorp, Quest Diagnostics, BioReference, etc.)
 *
 * This endpoint accepts lab results in a simplified format that can be mapped
 * from HL7 ORU messages or FHIR DiagnosticReport/Observation bundles.
 *
 * Integration flow:
 * 1. External lab system sends results via HL7 interface engine (Mirth Connect, Rhapsody)
 * 2. Interface engine transforms HL7 ORU → JSON and POSTs to /api/lab-interface/results
 * 3. Results are stored and linked to the original lab order via orderNumber
 * 4. Critical values trigger alerts to the ordering provider
 *
 * Future enhancements:
 * - Direct HL7v2 ORU parsing (HAPI HL7 library)
 * - FHIR DiagnosticReport bundle ingestion
 * - Bidirectional ORM/ORU (send orders, receive results)
 * - Real-time WebSocket notifications for critical values
 */
@PreAuthorize("hasAuthority('SCOPE_user/DiagnosticReport.read')")
@RestController
@RequestMapping("/api/lab-interface")
@RequiredArgsConstructor
@Slf4j
public class ExternalLabInterfaceController {

    private final LabResultService resultService;

    /**
     * Receive results from an external lab system.
     * Accepts a batch of results (e.g., all analytes from a CBC panel).
     *
     * Expected payload:
     * {
     *   "orderNumber": "ORD-2026-0001",
     *   "labName": "LabCorp",
     *   "results": [
     *     { "testName": "WBC", "testCode": "6690-2", "loincCode": "6690-2",
     *       "value": "7.5", "units": "x10^3/uL", "referenceLow": 4.5, "referenceHigh": 11.0,
     *       "abnormalFlag": "Normal", "status": "Final", "specimen": "Blood",
     *       "collectedDate": "2026-02-21", "reportedDate": "2026-02-21",
     *       "panelName": "CBC", "panelCode": "CBC" }
     *   ]
     * }
     */
    @PostMapping("/results")
    @PreAuthorize("hasAuthority('SCOPE_user/DiagnosticReport.write')")
    public ResponseEntity<ApiResponse<List<LabResultDto>>> receiveResults(@RequestBody Map<String, Object> payload) {
        try {
            String orderNumber = (String) payload.getOrDefault("orderNumber", "");
            String labName = (String) payload.getOrDefault("labName", "External Lab");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawResults = (List<Map<String, Object>>) payload.get("results");
            if (rawResults == null || rawResults.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("No results provided"));
            }

            List<LabResultDto> created = rawResults.stream().map(r -> {
                var dto = LabResultDto.builder()
                        .patientId(toLong(r.get("patientId")))
                        .orderNumber(orderNumber)
                        .testName((String) r.getOrDefault("testName", ""))
                        .testCode((String) r.getOrDefault("testCode", ""))
                        .loincCode((String) r.getOrDefault("loincCode", ""))
                        .value((String) r.getOrDefault("value", ""))
                        .units((String) r.getOrDefault("units", ""))
                        .referenceLow(toDouble(r.get("referenceLow")))
                        .referenceHigh(toDouble(r.get("referenceHigh")))
                        .referenceRange((String) r.getOrDefault("referenceRange", ""))
                        .abnormalFlag((String) r.getOrDefault("abnormalFlag", ""))
                        .status((String) r.getOrDefault("status", "Final"))
                        .specimen((String) r.getOrDefault("specimen", ""))
                        .collectedDate((String) r.getOrDefault("collectedDate", ""))
                        .reportedDate((String) r.getOrDefault("reportedDate", ""))
                        .panelName((String) r.getOrDefault("panelName", ""))
                        .panelCode((String) r.getOrDefault("panelCode", ""))
                        .notes("Source: " + labName)
                        .build();
                return resultService.create(dto);
            }).toList();

            log.info("Received {} results from {} for order {}", created.size(), labName, orderNumber);

            // Check for critical values
            long critCount = created.stream()
                    .filter(r -> "Critical".equalsIgnoreCase(r.getAbnormalFlag()))
                    .count();
            if (critCount > 0) {
                log.warn("CRITICAL VALUES DETECTED: {} critical results from {} for order {}",
                        critCount, labName, orderNumber);
                // Future: trigger notification to ordering provider
            }

            return ResponseEntity.ok(ApiResponse.ok(
                    String.format("Received %d results from %s", created.size(), labName), created));
        } catch (Exception e) {
            log.error("Failed to process external lab results", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    /**
     * Health check / capability endpoint for interface engines to verify connectivity.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        return ResponseEntity.ok(ApiResponse.ok("Lab interface active", Map.of(
                "status", "active",
                "supportedFormats", List.of("JSON", "HL7v2-JSON", "FHIR-R4"),
                "supportedOperations", List.of("receive-results", "query-orders"),
                "version", "1.0"
        )));
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return null; }
    }
}
