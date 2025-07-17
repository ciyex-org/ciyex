package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxGatherUsingAiRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxGatherUsingAiResponseDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxCallGatherUsingAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/gather-ai")
@RequiredArgsConstructor
public class TelnyxCallGatherUsingAiController {

    private final TelnyxCallGatherUsingAiService gatherAiService;

    @PostMapping("/{callControlId}")
    public ResponseEntity<TelnyxGatherUsingAiResponseDTO> gatherUsingAi(
            @PathVariable String callControlId,
            @RequestBody TelnyxGatherUsingAiRequestDTO request) {

        return ResponseEntity.ok(gatherAiService.gatherUsingAi(callControlId, request));
    }
}
