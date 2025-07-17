package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.service.telnyx.video.TelnyxDeleteRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/rooms")
@RequiredArgsConstructor
public class TelnyxDeleteRoomController {

    private final TelnyxDeleteRoomService deleteRoomService;

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable String roomId) {
        deleteRoomService.deleteRoom(roomId);
        return ResponseEntity.noContent().build(); // HTTP 204
    }
}
