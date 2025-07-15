package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.RoomResponseDto;
import com.qiaben.ciyex.dto.telnyx.UpdateRoomRequestDto;
import com.qiaben.ciyex.service.telnyx.UpdateRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/rooms")
@RequiredArgsConstructor
public class UpdateRoomController {

    private final UpdateRoomService updateRoomService;

    @PatchMapping("/{roomId}")
    public ResponseEntity<RoomResponseDto> updateRoom(
            @PathVariable String roomId,
            @RequestBody UpdateRoomRequestDto request) {

        RoomResponseDto response = updateRoomService.updateRoom(roomId, request);
        return ResponseEntity.ok(response);
    }


}
