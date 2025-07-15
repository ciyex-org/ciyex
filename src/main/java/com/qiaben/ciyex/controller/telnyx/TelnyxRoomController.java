package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.RoomDto;
import com.qiaben.ciyex.dto.telnyx.RoomRequestDto;
import com.qiaben.ciyex.dto.telnyx.RoomResponseDto;
import com.qiaben.ciyex.service.telnyx.TelnyxRoomService;
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
    public ResponseEntity<RoomDto> getRooms(@RequestParam Map<String, String> queryParams) {
        RoomDto roomList = telnyxRoomService.getRooms(queryParams);
        return ResponseEntity.ok(roomList);
    }

    @PostMapping
    public ResponseEntity<RoomResponseDto> createRoom(@RequestBody RoomRequestDto roomRequestDto) {
        RoomResponseDto room = telnyxRoomService.createRoom(roomRequestDto);
        return ResponseEntity.ok(room);
    }

}
