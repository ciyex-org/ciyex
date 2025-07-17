package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.video.TelnyxUnmuteParticipantsRequestDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxUnmuteParticipantsResponseDto;
import com.qiaben.ciyex.service.telnyx.video.TelnyxUnmuteParticipantsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/room-sessions")
@RequiredArgsConstructor
public class TelnyxUnmuteParticipantsController {

    private final TelnyxUnmuteParticipantsService service;

    @PostMapping("/{roomSessionId}/actions/unmute")
    public ResponseEntity<TelnyxUnmuteParticipantsResponseDto> unmuteParticipants(
            @PathVariable String roomSessionId,
            @RequestBody TelnyxUnmuteParticipantsRequestDto requestDto) {
        return ResponseEntity.ok(service.unmuteParticipants(roomSessionId, requestDto));
    }
}
