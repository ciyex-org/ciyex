package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomCompositionListResponseDto;
import com.qiaben.ciyex.service.telnyx.video.TelnyxRoomCompositionListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telnyx/room-compositions")
@RequiredArgsConstructor
public class TelnyxRoomCompositionListController {

    private final TelnyxRoomCompositionListService service;

    @GetMapping
    public TelnyxRoomCompositionListResponseDto getRoomCompositions(@RequestParam Map<String, String> filters) {
        return service.listRoomCompositions(filters);
    }
}
