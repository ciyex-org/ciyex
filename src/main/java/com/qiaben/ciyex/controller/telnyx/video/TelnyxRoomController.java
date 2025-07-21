package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomRequestDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomResponseDto;
import com.qiaben.ciyex.service.telnyx.video.TelnyxRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telnyx/rooms")
public class TelnyxRoomController {

    @Autowired
    private TelnyxRoomService telnyxRoomService;

    @GetMapping
    public ResponseEntity<TelnyxRoomDto> getRooms(@RequestParam Map<String, String> queryParams) {
        TelnyxRoomDto roomList = telnyxRoomService.getRooms(queryParams);
        return ResponseEntity.ok(roomList);
    }

    @PostMapping
    public ResponseEntity<TelnyxRoomResponseDto> createRoom(@RequestBody TelnyxRoomRequestDto roomRequestDto) {
        TelnyxRoomResponseDto room = telnyxRoomService.createRoom(roomRequestDto);
        return ResponseEntity.ok(room);
    }

}
