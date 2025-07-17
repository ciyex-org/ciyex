package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.video.TelnyxEndRoomSessionResponseDto;
import com.qiaben.ciyex.service.telnyx.video.TelnyxRoomSessionEndService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/room-sessions")
@RequiredArgsConstructor
public class TelnyxRoomSessionEndController {

    private final TelnyxRoomSessionEndService service;

    @PostMapping("/{sessionId}/end")
    public ResponseEntity<TelnyxEndRoomSessionResponseDto> endRoomSession(@PathVariable String sessionId) {
        TelnyxEndRoomSessionResponseDto resp = service.endSession(sessionId);
        return ResponseEntity.ok(resp);
    }
}
