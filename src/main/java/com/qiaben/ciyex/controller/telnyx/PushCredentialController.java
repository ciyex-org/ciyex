package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.PushCredentialResponseDTO;
import com.qiaben.ciyex.service.telnyx.PushCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx/push-credentials")
@RequiredArgsConstructor
public class PushCredentialController {

    private final PushCredentialService service;

    @GetMapping("/{id}")
    public PushCredentialResponseDTO getById(@PathVariable("id") String id) {
        return service.getPushCredentialById(id);
    }
}
