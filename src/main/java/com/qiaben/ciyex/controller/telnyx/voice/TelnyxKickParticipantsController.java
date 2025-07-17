package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxKickParticipantsRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxKickParticipantsResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxKickParticipantsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/room-sessions")
@RequiredArgsConstructor
public class TelnyxKickParticipantsController {

    private final TelnyxKickParticipantsService service;

    @PostMapping("/{roomSessionId}/actions/kick")
    public ResponseEntity<TelnyxKickParticipantsResponseDto> kickParticipants(
            @PathVariable String roomSessionId,
            @RequestBody TelnyxKickParticipantsRequestDto requestDto) {
        return ResponseEntity.ok(service.kickParticipants(roomSessionId, requestDto));
    }
}
