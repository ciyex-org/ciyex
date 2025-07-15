package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.PushCredentialsCreateDto;
import com.qiaben.ciyex.dto.telnyx.PushCredentialsResponseDto;
import com.qiaben.ciyex.service.telnyx.PushCredentialsCreateService; // ✅ FIXED PACKAGE
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/push-credentials/create")
@RequiredArgsConstructor
public class PushCredentialsCreateController {

    private final PushCredentialsCreateService service;

    @PostMapping
    public PushCredentialsResponseDto create(@RequestBody PushCredentialsCreateDto dto) {
        return service.createPushCredential(dto);
    }
}
