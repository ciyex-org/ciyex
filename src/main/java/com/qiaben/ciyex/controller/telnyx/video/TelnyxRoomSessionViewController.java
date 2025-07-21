package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomSessionResponseDto;
import com.qiaben.ciyex.service.telnyx.video.TelnyxRoomSessionViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/room-sessions")
@RequiredArgsConstructor
public class TelnyxRoomSessionViewController {

    private final TelnyxRoomSessionViewService service;

    @GetMapping("/{sessionId}")
    public ResponseEntity<TelnyxRoomSessionResponseDto> getRoomSession(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "false") boolean includeParticipants
    ) {
        TelnyxRoomSessionResponseDto response = service.getRoomSessionById(sessionId, includeParticipants);
        return ResponseEntity.ok(response);
    }
}
