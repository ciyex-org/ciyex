// controller/telnyx/TelnyxRoomSessionMuteController.java
package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ActionResultDto;
import com.qiaben.ciyex.dto.telnyx.MuteRequestDto;
import com.qiaben.ciyex.service.telnyx.TelnyxRoomSessionMuteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/room-sessions")
@RequiredArgsConstructor
public class TelnyxRoomSessionMuteController {

    private final TelnyxRoomSessionMuteService service;

    @PostMapping("/{sessionId}/mute")
    public ResponseEntity<ActionResultDto> mute(
            @PathVariable String sessionId,
            @RequestBody MuteRequestDto request) {

        ActionResultDto resp = service.mute(sessionId, request);
        return ResponseEntity.ok(resp);
    }
}
