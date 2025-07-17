package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxPushCredentialsListResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxPushCredentialsListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/push-credentials")
@RequiredArgsConstructor
public class TelnyxPushCredentialsListController {

    private final TelnyxPushCredentialsListService service;

    @GetMapping
    public TelnyxPushCredentialsListResponseDto list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String alias,
            @RequestParam(name = "page[size]", required = false) Integer pageSize,
            @RequestParam(name = "page[number]", required = false) Integer pageNumber
    ) {
        return service.listPushCredentials(type, alias, pageSize, pageNumber);
    }
}
