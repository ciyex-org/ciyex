package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.HistoryOfPresentIllnessDto;
import com.qiaben.ciyex.service.HistoryOfPresentIllnessService;
import com.qiaben.ciyex.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{encounterId}/history-of-present-illness")  // Dynamic encounterId in the URL path
public class HistoryOfPresentIllnessController {

    private final HistoryOfPresentIllnessService historyOfPresentIllnessService;

    public HistoryOfPresentIllnessController(HistoryOfPresentIllnessService historyOfPresentIllnessService) {
        this.historyOfPresentIllnessService = historyOfPresentIllnessService;
    }

    // Create a new History of Present Illness entry
    @PostMapping
    public ResponseEntity<ApiResponse<HistoryOfPresentIllnessDto>> create(
            @PathVariable Long encounterId,
            @RequestBody HistoryOfPresentIllnessDto dto,
            @RequestHeader("orgid") Long orgId  // Pass orgId in the header
    ) {
        dto.setOrgId(orgId);  // Set orgId from header
        dto.setEncounterId(encounterId);  // Set encounterId from URL path
        HistoryOfPresentIllnessDto createdHPI = historyOfPresentIllnessService.create(dto);
        ApiResponse<HistoryOfPresentIllnessDto> response = new ApiResponse.Builder<HistoryOfPresentIllnessDto>()
                .success(true)
                .message("History of Present Illness created successfully")
                .data(createdHPI)
                .build();
        return ResponseEntity.ok(response);
    }

    // Get all History of Present Illness entries for a specific encounter
    @GetMapping
    public ResponseEntity<ApiResponse<List<HistoryOfPresentIllnessDto>>> getAll(@PathVariable Long encounterId) {
        List<HistoryOfPresentIllnessDto> hpis = historyOfPresentIllnessService.getByEncounterId(encounterId);
        ApiResponse<List<HistoryOfPresentIllnessDto>> response = new ApiResponse.Builder<List<HistoryOfPresentIllnessDto>>()
                .success(true)
                .message("History of Present Illness entries fetched successfully")
                .data(hpis)
                .build();
        return ResponseEntity.ok(response);
    }

    // Update a specific History of Present Illness entry
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HistoryOfPresentIllnessDto>> update(
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestBody HistoryOfPresentIllnessDto dto
    ) {
        HistoryOfPresentIllnessDto updatedHPI = historyOfPresentIllnessService.update(encounterId, id, dto);
        ApiResponse<HistoryOfPresentIllnessDto> response = new ApiResponse.Builder<HistoryOfPresentIllnessDto>()
                .success(true)
                .message("History of Present Illness updated successfully")
                .data(updatedHPI)
                .build();
        return ResponseEntity.ok(response);
    }

    // Delete a specific History of Present Illness entry
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long encounterId, @PathVariable Long id) {
        historyOfPresentIllnessService.delete(encounterId, id);
        ApiResponse<Void> response = new ApiResponse.Builder<Void>()
                .success(true)
                .message("History of Present Illness deleted successfully")
                .data(null)
                .build();
        return ResponseEntity.ok(response);
    }
}
