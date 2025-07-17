package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxGatherRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxGatherResponseDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxGatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/gather")
@RequiredArgsConstructor
public class TelnyxGatherController {

    private final TelnyxGatherService gatherService;

    @PostMapping("/{callControlId}")
    public ResponseEntity<TelnyxGatherResponseDTO> gatherDigits(
            @PathVariable String callControlId,
            @RequestBody TelnyxGatherRequestDTO request
    ) {
        return ResponseEntity.ok(gatherService.gatherDigits(callControlId, request));
    }
}
