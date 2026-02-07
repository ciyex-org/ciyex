package com.qiaben.ciyex.eligibility.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.eligibility.dto.EligibilityRequestDto;
import com.qiaben.ciyex.eligibility.dto.EligibilityResponseDto;
import com.qiaben.ciyex.eligibility.service.EligibilityService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/eligibility")
@Slf4j
public class EligibilityController {
    
    private final EligibilityService eligibilityService;
    
    public EligibilityController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }
    
    @PostMapping("/check/{patientId}")
    public ResponseEntity<ApiResponse<EligibilityResponseDto>> checkEligibility(
            @PathVariable Long patientId,
            @RequestParam(required = false) String serviceTypeCode) {
        try {
            log.info("Eligibility check for patient: {}", patientId);
            EligibilityResponseDto response = eligibilityService.checkEligibility(patientId, serviceTypeCode);
            
            return ResponseEntity.ok(ApiResponse.<EligibilityResponseDto>builder()
                    .success(true)
                    .message("Eligibility check completed successfully")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Eligibility check failed: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<EligibilityResponseDto>builder()
                    .success(false)
                    .message("Eligibility check failed: " + e.getMessage())
                    .build());
        }
    }
}
