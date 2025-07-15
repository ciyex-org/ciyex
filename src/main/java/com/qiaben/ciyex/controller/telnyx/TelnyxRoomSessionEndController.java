package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.EndRoomSessionResponseDto;
import com.qiaben.ciyex.service.telnyx.TelnyxRoomSessionEndService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/room-sessions")
@RequiredArgsConstructor
public class TelnyxRoomSessionEndController {

    private final TelnyxRoomSessionEndService service;

    @PostMapping("/{sessionId}/end")
    public ResponseEntity<EndRoomSessionResponseDto> endRoomSession(@PathVariable String sessionId) {
        EndRoomSessionResponseDto resp = service.endSession(sessionId);
        return ResponseEntity.ok(resp);
    }
}
