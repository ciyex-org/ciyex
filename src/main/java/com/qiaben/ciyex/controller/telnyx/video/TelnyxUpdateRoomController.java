package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomResponseDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxUpdateRoomRequestDto;
import com.qiaben.ciyex.service.telnyx.video.TelnyxUpdateRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/rooms")
@RequiredArgsConstructor
public class TelnyxUpdateRoomController {

    private final TelnyxUpdateRoomService updateRoomService;

    @PatchMapping("/{roomId}")
    public ResponseEntity<TelnyxRoomResponseDto> updateRoom(
            @PathVariable String roomId,
            @RequestBody TelnyxUpdateRoomRequestDto request) {

        TelnyxRoomResponseDto response = updateRoomService.updateRoom(roomId, request);
        return ResponseEntity.ok(response);
    }


}
