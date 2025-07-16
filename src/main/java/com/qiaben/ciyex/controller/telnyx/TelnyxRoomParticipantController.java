package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.RoomParticipantListDto;
import com.qiaben.ciyex.service.telnyx.TelnyxRoomParticipantService;
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
    public ResponseEntity<RoomParticipantListDto> listParticipants(
            @PathVariable String roomSessionId,
            @RequestParam(name = "filter[date_joined_at][gte]", required = false) String joinedGte,
            @RequestParam(name = "filter[context]", required = false) String context,
            @RequestParam(name = "page[size]", defaultValue = "20") int pageSize,
            @RequestParam(name = "page[number]", defaultValue = "1") int pageNumber) {

        RoomParticipantListDto resp = service.listParticipants(
                roomSessionId,
                Optional.ofNullable(joinedGte),
                Optional.ofNullable(context),
                pageSize,
                pageNumber);

        return ResponseEntity.ok(resp);
    }
}
