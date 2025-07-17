package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomSessionListResponseDto;
import com.qiaben.ciyex.service.telnyx.video.TelnyxRoomSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/telnyx/room-sessions")
@RequiredArgsConstructor
public class TelnyxRoomSessionController {

    private final TelnyxRoomSessionService service;

    @GetMapping
    public ResponseEntity<TelnyxRoomSessionListResponseDto> listRoomSessions(
            @RequestParam(name = "filter[room_id]", required = false) String roomId,
            @RequestParam(name = "filter[date_created_at][gte]", required = false) String createdGte,
            @RequestParam(name = "filter[date_created_at][lte]", required = false) String createdLte,
            @RequestParam(name = "filter[active]",          required = false) Boolean active,
            @RequestParam(name = "include_participants",    defaultValue = "false") boolean includeParticipants,
            @RequestParam(name = "page[size]",              defaultValue = "20")  int pageSize,
            @RequestParam(name = "page[number]",            defaultValue = "1")   int pageNumber) {

        TelnyxRoomSessionListResponseDto resp = service.listRoomSessions(
                Optional.ofNullable(roomId),
                Optional.ofNullable(createdGte),
                Optional.ofNullable(createdLte),
                Optional.ofNullable(active),
                includeParticipants, pageSize, pageNumber);

        return ResponseEntity.ok(resp);
    }
}
