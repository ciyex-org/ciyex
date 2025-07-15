package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.CreateRoomCompositionRequestDto;
import com.qiaben.ciyex.dto.telnyx.CreateRoomCompositionResponseDto;
import com.qiaben.ciyex.service.telnyx.CreateRoomCompositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/room-compositions")
@RequiredArgsConstructor
public class CreateRoomCompositionController {

    private final CreateRoomCompositionService service;

    @PostMapping
    public CreateRoomCompositionResponseDto create(@RequestBody CreateRoomCompositionRequestDto request) {
        return service.createComposition(request);
    }
}
