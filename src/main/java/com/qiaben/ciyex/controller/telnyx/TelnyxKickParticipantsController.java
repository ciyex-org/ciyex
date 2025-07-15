package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.KickParticipantsRequestDto;
import com.qiaben.ciyex.dto.telnyx.KickParticipantsResponseDto;
import com.qiaben.ciyex.service.telnyx.TelnyxKickParticipantsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/room-sessions")
@RequiredArgsConstructor
public class TelnyxKickParticipantsController {

    private final TelnyxKickParticipantsService service;

    @PostMapping("/{roomSessionId}/actions/kick")
    public ResponseEntity<KickParticipantsResponseDto> kickParticipants(
            @PathVariable String roomSessionId,
            @RequestBody KickParticipantsRequestDto requestDto) {
        return ResponseEntity.ok(service.kickParticipants(roomSessionId, requestDto));
    }
}
