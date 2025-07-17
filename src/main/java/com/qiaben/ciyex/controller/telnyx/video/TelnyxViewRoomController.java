package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.video.TelnyxViewRoomDto;
import com.qiaben.ciyex.service.telnyx.video.TelnyxViewRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/rooms")
@RequiredArgsConstructor
public class TelnyxViewRoomController {

    private final TelnyxViewRoomService viewRoomService;

    @GetMapping("/{roomId}")
    public ResponseEntity<TelnyxViewRoomDto> getRoom(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "false") boolean include_sessions
    ) {
        TelnyxViewRoomDto room = viewRoomService.getRoomDetails(roomId, include_sessions);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(room);
    }
}
