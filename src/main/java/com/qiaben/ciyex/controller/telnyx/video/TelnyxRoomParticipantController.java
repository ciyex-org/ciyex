package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomParticipantListDto;
import com.qiaben.ciyex.service.telnyx.video.TelnyxRoomParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/telnyx/room-sessions")
@RequiredArgsConstructor
public class TelnyxRoomParticipantController {

    private final TelnyxRoomParticipantService service;

    @GetMapping("/{roomSessionId}/participants")
    public ResponseEntity<TelnyxRoomParticipantListDto> listParticipants(
            @PathVariable String roomSessionId,
            @RequestParam(name = "filter[date_joined_at][gte]", required = false) String joinedGte,
            @RequestParam(name = "filter[context]", required = false) String context,
            @RequestParam(name = "page[size]", defaultValue = "20") int pageSize,
            @RequestParam(name = "page[number]", defaultValue = "1") int pageNumber) {

        TelnyxRoomParticipantListDto resp = service.listParticipants(
                roomSessionId,
                Optional.ofNullable(joinedGte),
                Optional.ofNullable(context),
                pageSize,
                pageNumber);

        return ResponseEntity.ok(resp);
    }
}
