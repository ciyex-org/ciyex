package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxForkStartRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxForkStartResponseDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxForkStartService;
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
