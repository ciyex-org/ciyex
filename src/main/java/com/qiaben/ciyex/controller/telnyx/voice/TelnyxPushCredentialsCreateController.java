package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxPushCredentialsCreateDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxPushCredentialsResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxPushCredentialsCreateService; // ✅ FIXED PACKAGE
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/push-credentials/create")
@RequiredArgsConstructor
public class TelnyxPushCredentialsCreateController {

    private final TelnyxPushCredentialsCreateService service;

    @PostMapping
    public TelnyxPushCredentialsResponseDto create(@RequestBody TelnyxPushCredentialsCreateDto dto) {
        return service.createPushCredential(dto);
    }
}
