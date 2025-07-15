package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.TelnyxForkStartRequestDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxForkStartResponseDTO;
import com.qiaben.ciyex.service.telnyx.TelnyxForkStartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/fork")
@RequiredArgsConstructor
public class TelnyxForkStartController {

    private final TelnyxForkStartService forkStartService;

    @PostMapping("/{callControlId}")
    public ResponseEntity<TelnyxForkStartResponseDTO> startForking(
            @PathVariable String callControlId,
            @RequestBody TelnyxForkStartRequestDTO request
    ) {
        return ResponseEntity.ok(forkStartService.startForking(callControlId, request));
    }
}
