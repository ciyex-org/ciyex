package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxPushCredentialResponseDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxPushCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx/push-credentials")
@RequiredArgsConstructor
public class TelnyxPushCredentialController {

    private final TelnyxPushCredentialService service;

    @GetMapping("/{id}")
    public TelnyxPushCredentialResponseDTO getById(@PathVariable("id") String id) {
        return service.getPushCredentialById(id);
    }
}
