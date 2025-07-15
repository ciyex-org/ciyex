package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.RoomSessionResponseDto;
import com.qiaben.ciyex.service.telnyx.TelnyxRoomSessionViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/room-sessions")
@RequiredArgsConstructor
public class TelnyxRoomSessionViewController {

    private final TelnyxRoomSessionViewService service;

    @GetMapping("/{sessionId}")
    public ResponseEntity<RoomSessionResponseDto> getRoomSession(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "false") boolean includeParticipants
    ) {
        RoomSessionResponseDto response = service.getRoomSessionById(sessionId, includeParticipants);
        return ResponseEntity.ok(response);
    }
}
