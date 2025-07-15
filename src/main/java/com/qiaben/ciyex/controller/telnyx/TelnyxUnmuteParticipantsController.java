package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.UnmuteParticipantsRequestDto;
import com.qiaben.ciyex.dto.telnyx.UnmuteParticipantsResponseDto;
import com.qiaben.ciyex.service.telnyx.TelnyxUnmuteParticipantsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/room-sessions")
@RequiredArgsConstructor
public class TelnyxUnmuteParticipantsController {

    private final TelnyxUnmuteParticipantsService service;

    @PostMapping("/{roomSessionId}/actions/unmute")
    public ResponseEntity<UnmuteParticipantsResponseDto> unmuteParticipants(
            @PathVariable String roomSessionId,
            @RequestBody UnmuteParticipantsRequestDto requestDto) {
        return ResponseEntity.ok(service.unmuteParticipants(roomSessionId, requestDto));
    }
}
