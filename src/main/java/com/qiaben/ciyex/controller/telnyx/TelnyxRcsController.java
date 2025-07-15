// src/main/java/com/qiaben/ciyex/controller/telnyx/TelnyxRcsController.java
package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.RcsCapabilitiesDTO;
import com.qiaben.ciyex.dto.telnyx.RcsTestNumberInviteDTO;
import com.qiaben.ciyex.service.telnyx.TelnyxRcsService;
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
    public ResponseEntity<RcsCapabilitiesDTO> getCapabilities(
            @PathVariable String agentId,
            @PathVariable String phoneNumber) {
        return ResponseEntity.ok(rcsService.listCapabilities(agentId, phoneNumber));
    }

    /**
     * POST /api/telnyx/rcs/{agentId}/{phoneNumber}
     */
    @PostMapping("/{agentId}/{phoneNumber}")
    public ResponseEntity<RcsTestNumberInviteDTO> inviteTestNumber(
            @PathVariable String agentId,
            @PathVariable String phoneNumber) {
        return ResponseEntity.ok(rcsService.inviteTestNumber(agentId, phoneNumber));
    }
}
