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
    
    @PostMapping("/check")
    public ResponseEntity<ApiResponse<EligibilityResponseDto>> checkEligibility(
            @Valid @RequestBody EligibilityRequestDto request) {
        try {
            log.info("Eligibility check request for member: {}", request.getMemberId());
            EligibilityResponseDto response = eligibilityService.checkEligibility(request);
            
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
    
    @PostMapping("/generate-270")
    public ResponseEntity<ApiResponse<String>> generate270(
            @Valid @RequestBody EligibilityRequestDto request) {
        try {
            String x12Request = eligibilityService.generateX12Request(request);
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true)
                    .message("X12 270 request generated successfully")
                    .data(x12Request)
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate X12 270: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to generate X12 270: " + e.getMessage())
                    .build());
        }
    }
    
    @PostMapping("/parse-271")
    public ResponseEntity<ApiResponse<EligibilityResponseDto>> parse271(
            @RequestBody String x12Response) {
        try {
            EligibilityResponseDto response = eligibilityService.parseX12Response(x12Response);
            return ResponseEntity.ok(ApiResponse.<EligibilityResponseDto>builder()
                    .success(true)
                    .message("X12 271 response parsed successfully")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Failed to parse X12 271: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<EligibilityResponseDto>builder()
                    .success(false)
                    .message("Failed to parse X12 271: " + e.getMessage())
                    .build());
        }
    }
}
