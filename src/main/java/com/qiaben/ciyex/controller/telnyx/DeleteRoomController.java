package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.service.telnyx.DeleteRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/rooms")
@RequiredArgsConstructor
public class DeleteRoomController {

    private final DeleteRoomService deleteRoomService;

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable String roomId) {
        deleteRoomService.deleteRoom(roomId);
        return ResponseEntity.noContent().build(); // HTTP 204
    }
}
