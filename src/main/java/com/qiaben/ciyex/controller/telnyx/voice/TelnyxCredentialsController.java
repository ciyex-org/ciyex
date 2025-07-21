package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCredentialsDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCredentialsListResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxCredentialsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/credentials")
@RequiredArgsConstructor
public class TelnyxCredentialsController {

    private final TelnyxCredentialsService service;

    @GetMapping
    public TelnyxCredentialsListResponseDto list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "resource_id") String resourceId,
            @RequestParam(required = false, name = "sip_username") String sipUsername
    ) {
        return service.list(page, size, tag, name, status, resourceId, sipUsername);
    }

    @PostMapping
    public TelnyxCredentialsDto create(@RequestBody TelnyxCredentialsDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public TelnyxCredentialsDto update(@PathVariable String id, @RequestBody TelnyxCredentialsDto dto) {
        return service.update(id, dto);
    }

    @GetMapping("/{id}")
    public TelnyxCredentialsDto getById(@PathVariable String id) {
        return service.getById(id);
    }
}
