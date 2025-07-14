package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.TelnyxGatherUsingAiRequestDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxGatherUsingAiResponseDTO;
import com.qiaben.ciyex.service.telnyx.TelnyxCallGatherUsingAiService;
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
