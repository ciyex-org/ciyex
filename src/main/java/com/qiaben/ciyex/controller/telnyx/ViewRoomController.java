package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ViewRoomDto;
import com.qiaben.ciyex.service.telnyx.ViewRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/rooms")
@RequiredArgsConstructor
public class ViewRoomController {

    private final ViewRoomService viewRoomService;

    @GetMapping("/{roomId}")
    public ResponseEntity<ViewRoomDto> getRoom(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "false") boolean include_sessions
    ) {
        ViewRoomDto room = viewRoomService.getRoomDetails(roomId, include_sessions);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(room);
    }
}
