// controller/telnyx/TelnyxRoomSessionMuteController.java
package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxActionResultDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxMuteRequestDto;
import com.qiaben.ciyex.service.telnyx.video.TelnyxRoomSessionMuteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/room-sessions")
@RequiredArgsConstructor
public class TelnyxRoomSessionMuteController {

    private final TelnyxRoomSessionMuteService service;

    @PostMapping("/{sessionId}/mute")
    public ResponseEntity<TelnyxActionResultDto> mute(
            @PathVariable String sessionId,
            @RequestBody TelnyxMuteRequestDto request) {

        TelnyxActionResultDto resp = service.mute(sessionId, request);
        return ResponseEntity.ok(resp);
    }
}
