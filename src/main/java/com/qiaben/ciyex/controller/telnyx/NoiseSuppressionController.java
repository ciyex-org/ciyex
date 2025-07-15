package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.NoiseSuppressionStartRequestDTO;
import com.qiaben.ciyex.dto.telnyx.NoiseSuppressionStopRequestDTO;
import com.qiaben.ciyex.service.telnyx.NoiseSuppressionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/noise-suppression")
@RequiredArgsConstructor
public class NoiseSuppressionController {

    private final NoiseSuppressionService service;

    @PostMapping("/{callControlId}/start")
    public ResponseEntity<String> start(
            @PathVariable String callControlId,
            @RequestBody NoiseSuppressionStartRequestDTO dto) {

        return ResponseEntity.ok(service.start(callControlId, dto));
    }

    @PostMapping("/{callControlId}/stop")
    public ResponseEntity<String> stop(
            @PathVariable String callControlId,
            @RequestBody NoiseSuppressionStopRequestDTO dto) {

        return ResponseEntity.ok(service.stop(callControlId, dto));
    }
}

