

package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.EncounterDto;
import com.qiaben.ciyex.entity.EncounterStatus;
import com.qiaben.ciyex.service.EncounterBrowserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
            Pageable pageable
    ) {
        Optional<String> statusOpt = Optional.empty();
        String normalized = status == null ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
        if (!"ALL".equals(normalized)) {
            statusOpt = Optional.of(normalized);
        }

        Page<EncounterDto> page = EncounterBrowserService.listAll(
                statusOpt, recentOnly, recentCount, pageable
        );

        return ResponseEntity.ok(
                ApiResponse.<Page<EncounterDto>>builder()
                        .success(true)
                        .message("Encounters fetched")
                        .data(page)
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
