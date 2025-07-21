package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxFqdnConnectionDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxFqdnConnectionListResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxFqdnConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/fqdn-connections")
@RequiredArgsConstructor
public class TelnyxFqdnConnectionController {

    private final TelnyxFqdnConnectionService service;

    @GetMapping
    public TelnyxFqdnConnectionListResponseDto list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "250") int size,
            @RequestParam(required = false) String connectionId,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) Integer port
    ) {
        return service.list(connectionId, ipAddress, port, page, size);
    }

    @PostMapping
    public TelnyxFqdnConnectionDto create(@RequestBody TelnyxFqdnConnectionDto dto) {
        return service.create(dto);
    }

    @PatchMapping("/{id}")
    public TelnyxFqdnConnectionDto update(@PathVariable String id, @RequestBody TelnyxFqdnConnectionDto dto) {
        return service.update(id, dto);
    }

    @GetMapping("/{id}")
    public TelnyxFqdnConnectionDto get(@PathVariable String id) {
        return service.getById(id);
    }
}
