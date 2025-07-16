package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.RoomCompositionViewResponseDto;
import com.qiaben.ciyex.service.telnyx.RoomCompositionViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/room-compositions")
@RequiredArgsConstructor
public class RoomCompositionViewController {

    private final RoomCompositionViewService roomCompositionViewService;

    @GetMapping("/{id}")
    public RoomCompositionViewResponseDto getRoomComposition(@PathVariable("id") String id) {
        return roomCompositionViewService.getRoomComposition(id);
    }
}
