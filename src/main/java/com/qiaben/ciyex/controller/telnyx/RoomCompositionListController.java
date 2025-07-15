package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.RoomCompositionListResponseDto;
import com.qiaben.ciyex.service.telnyx.RoomCompositionListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telnyx/room-compositions")
@RequiredArgsConstructor
public class RoomCompositionListController {

    private final RoomCompositionListService service;

    @GetMapping
    public RoomCompositionListResponseDto getRoomCompositions(@RequestParam Map<String, String> filters) {
        return service.listRoomCompositions(filters);
    }
}
