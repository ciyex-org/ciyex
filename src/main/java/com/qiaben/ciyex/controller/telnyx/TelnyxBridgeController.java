package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.TelnyxBridgeRequestDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxBridgeResponseDTO;
import com.qiaben.ciyex.service.telnyx.TelnyxBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/bridge")
@RequiredArgsConstructor
public class TelnyxBridgeController {

    private final TelnyxBridgeService bridgeService;

    @PostMapping("/{callControlId}")
    public ResponseEntity<TelnyxBridgeResponseDTO> bridgeCall(
            @PathVariable String callControlId,
            @RequestBody TelnyxBridgeRequestDTO body
    ) {
        return ResponseEntity.ok(bridgeService.bridgeCall(callControlId, body));
    }
}
