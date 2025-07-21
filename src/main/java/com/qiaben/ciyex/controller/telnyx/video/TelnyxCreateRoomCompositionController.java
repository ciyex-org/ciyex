package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.video.TelnyxCreateRoomCompositionRequestDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxCreateRoomCompositionResponseDto;
import com.qiaben.ciyex.service.telnyx.video.TelnyxCreateRoomCompositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/room-compositions")
@RequiredArgsConstructor
public class TelnyxCreateRoomCompositionController {

    private final TelnyxCreateRoomCompositionService service;

    @PostMapping
    public TelnyxCreateRoomCompositionResponseDto create(@RequestBody TelnyxCreateRoomCompositionRequestDto request) {
        return service.createComposition(request);
    }
}
