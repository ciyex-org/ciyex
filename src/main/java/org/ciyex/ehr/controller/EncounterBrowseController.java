

package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.EncounterDto;
import org.ciyex.ehr.dto.EncounterStatus;
import org.ciyex.ehr.service.EncounterBrowserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/api/encounters") // NEW: org-wide browse
public class EncounterBrowseController {

    private final EncounterBrowserService EncounterBrowserService;

    @Autowired
    public EncounterBrowseController(EncounterBrowserService encounterBrowserService) {
        this.EncounterBrowserService = encounterBrowserService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EncounterDto>>> listAll(
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(name = "recentOnly", required = false, defaultValue = "false") boolean recentOnly,
            @RequestParam(name = "recentCount", required = false, defaultValue = "10") int recentCount,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Optional<String> statusOpt = Optional.empty();
        String normalized = status == null ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
        if (!"ALL".equals(normalized)) {
            statusOpt = Optional.of(normalized);
        }

        Page<EncounterDto> result = EncounterBrowserService.listAll(
                statusOpt, recentOnly, recentCount, pageable
        );

        return ResponseEntity.ok(
                ApiResponse.<Page<EncounterDto>>builder()
                        .success(true)
                        .message("Encounters fetched")
                        .data(result)
                        .build()
        );
    }

    @GetMapping("report/encounterAll")
    public ResponseEntity<ApiResponse<?>> getAllEncounters() {
        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message("All encounters fetched")
                        .data(EncounterBrowserService.getAllEncounters())
                        .build()
        );
    }
}
