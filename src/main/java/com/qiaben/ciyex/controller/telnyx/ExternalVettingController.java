package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.*;
import com.qiaben.ciyex.service.telnyx.ExternalVettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/external-vetting")
@RequiredArgsConstructor
public class ExternalVettingController {

    private final ExternalVettingService service;

    @PostMapping("/{brandId}/import")
    public ResponseEntity<ExternalVettingResponseDto> importVetting(
            @PathVariable String brandId,
            @RequestBody ExternalVettingRequestDto request
    ) {
        return ResponseEntity.ok(service.importVetting(brandId, request));
    }

    @PostMapping("/{brandId}/order")
    public ResponseEntity<Object> orderVetting(
            @PathVariable String brandId,
            @RequestBody VettingClassRequestDto request
    ) {
        return ResponseEntity.ok(service.orderVetting(brandId, request));
    }

    @GetMapping("/{brandId}/feedback")
    public ResponseEntity<BrandFeedbackResponseDto> getFeedback(
            @PathVariable String brandId
    ) {
        return ResponseEntity.ok(service.getFeedback(brandId));
    }
}
