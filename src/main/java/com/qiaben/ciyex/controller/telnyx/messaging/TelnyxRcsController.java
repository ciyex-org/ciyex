// src/main/java/com/qiaben/ciyex/controller/telnyx/TelnyxRcsController.java
package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxRcsCapabilitiesDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxRcsTestNumberInviteDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxRcsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/rcs")
@RequiredArgsConstructor
public class TelnyxRcsController {

    private final TelnyxRcsService rcsService;

    /**
     * GET /api/telnyx/rcs/{agentId}/{phoneNumber}
     */
    @GetMapping("/{agentId}/{phoneNumber}")
    public ResponseEntity<TelnyxRcsCapabilitiesDTO> getCapabilities(
            @PathVariable String agentId,
            @PathVariable String phoneNumber) {
        return ResponseEntity.ok(rcsService.listCapabilities(agentId, phoneNumber));
    }

    /**
     * POST /api/telnyx/rcs/{agentId}/{phoneNumber}
     */
    @PostMapping("/{agentId}/{phoneNumber}")
    public ResponseEntity<TelnyxRcsTestNumberInviteDTO> inviteTestNumber(
            @PathVariable String agentId,
            @PathVariable String phoneNumber) {
        return ResponseEntity.ok(rcsService.inviteTestNumber(agentId, phoneNumber));
    }
}
