package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.RoomParticipantDto;
import com.qiaben.ciyex.service.telnyx.RoomParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telnyx/room-participants")
@RequiredArgsConstructor
public class RoomParticipantController {

    private final RoomParticipantService roomParticipantService;

    @GetMapping
    public RoomParticipantDto.RoomParticipantListResponse listRoomParticipants(@RequestParam Map<String, String> queryParams) {
        return roomParticipantService.getRoomParticipants(queryParams);
    }

    @GetMapping("/{id}")
    public RoomParticipantDto.RoomParticipantSingleResponse getRoomParticipant(@PathVariable String id) {
        return roomParticipantService.getRoomParticipantById(id);
    }
}
