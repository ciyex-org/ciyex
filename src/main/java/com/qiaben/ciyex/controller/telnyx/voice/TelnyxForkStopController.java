package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxForkStopRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxForkStopResponseDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxForkStopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/fork-stop")
@RequiredArgsConstructor
public class TelnyxForkStopController {

    private final TelnyxForkStopService forkStopService;

    @PostMapping("/{callControlId}")
    public ResponseEntity<TelnyxForkStopResponseDTO> stopForking(
            @PathVariable String callControlId,
            @RequestBody TelnyxForkStopRequestDTO request
    ) {
        return ResponseEntity.ok(forkStopService.stopForking(callControlId, request));
    }
}
