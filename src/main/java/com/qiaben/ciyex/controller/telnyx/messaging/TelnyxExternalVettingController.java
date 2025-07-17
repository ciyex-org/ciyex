package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxExternalVettingRequestDto;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxExternalVettingResponseDto;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxVettingClassRequestDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxExternalVettingService;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxBrandFeedbackResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/external-vetting")
@RequiredArgsConstructor
public class TelnyxExternalVettingController {

    private final TelnyxExternalVettingService service;

    @PostMapping("/{brandId}/import")
    public ResponseEntity<TelnyxExternalVettingResponseDto> importVetting(
            @PathVariable String brandId,
            @RequestBody TelnyxExternalVettingRequestDto request
    ) {
        return ResponseEntity.ok(service.importVetting(brandId, request));
    }

    @PostMapping("/{brandId}/order")
    public ResponseEntity<Object> orderVetting(
            @PathVariable String brandId,
            @RequestBody TelnyxVettingClassRequestDto request
    ) {
        return ResponseEntity.ok(service.orderVetting(brandId, request));
    }

    @GetMapping("/{brandId}/feedback")
    public ResponseEntity<TelnyxBrandFeedbackResponseDto> getFeedback(
            @PathVariable String brandId
    ) {
        return ResponseEntity.ok(service.getFeedback(brandId));
    }
}
