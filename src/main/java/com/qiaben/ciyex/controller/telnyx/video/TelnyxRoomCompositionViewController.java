package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomCompositionViewResponseDto;
import com.qiaben.ciyex.service.telnyx.video.TelnyxRoomCompositionViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/room-compositions")
@RequiredArgsConstructor
public class TelnyxRoomCompositionViewController {

    private final TelnyxRoomCompositionViewService roomCompositionViewService;

    @GetMapping("/{id}")
    public TelnyxRoomCompositionViewResponseDto getRoomComposition(@PathVariable("id") String id) {
        return roomCompositionViewService.getRoomComposition(id);
    }
}
