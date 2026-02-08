package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.EncounterSummaryDto;
import org.ciyex.ehr.service.EncounterSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/encounters")
@RequiredArgsConstructor
public class EncounterSummaryController {

    private final EncounterSummaryService encounterSummaryService;

    @GetMapping(value = "/{patientId}/{encounterId}/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<EncounterSummaryDto>> getSummary(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        try {
            EncounterSummaryDto summary = encounterSummaryService.load(patientId, encounterId);
            return ResponseEntity.ok(ApiResponse.<EncounterSummaryDto>builder()
                    .success(true)
                    .message("Encounter summary fetched")
                    .data(summary)
                    .build());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(ApiResponse.<EncounterSummaryDto>builder()
                    .success(false)
                    .message("Error fetching summary: " + ex.getMessage())
                    .build());
        }
    }

    @GetMapping(value = "/{patientId}/{encounterId}/summary/print", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PDF_VALUE})
    public ResponseEntity<?> getPrintSummary(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) String acceptHeader) {
        try {
            EncounterSummaryDto summary = encounterSummaryService.load(patientId, encounterId);
            
            if (acceptHeader.contains(MediaType.APPLICATION_PDF_VALUE)) {
                String html = encounterSummaryService.buildHtml(summary);
                byte[] pdf = encounterSummaryService.renderPdfFromHtml(html);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("inline", "encounter-summary.pdf");
                
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(pdf);
            } else {
                return ResponseEntity.ok(ApiResponse.<EncounterSummaryDto>builder()
                        .success(true)
                        .message("Encounter summary for print fetched")
                        .data(summary)
                        .build());
            }
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(ApiResponse.<EncounterSummaryDto>builder()
                    .success(false)
                    .message("Error fetching print summary: " + ex.getMessage())
                    .build());
        }
    }
}
