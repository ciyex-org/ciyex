package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxIpDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxIpListResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxIpService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/ips")
@RequiredArgsConstructor
public class TelnyxIpController {

    private final TelnyxIpService service;

    @GetMapping
    public TelnyxIpListResponseDto list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "250") int size,
            @RequestParam(required = false, name = "connection_id") String connectionId,
            @RequestParam(required = false, name = "ip_address") String ipAddress,
            @RequestParam(required = false) Integer port
    ) {
        return service.list(page, size, connectionId, ipAddress, port);
    }

    @PostMapping
    public TelnyxIpDto create(@RequestBody TelnyxIpDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public TelnyxIpDto update(@PathVariable String id, @RequestBody TelnyxIpDto dto) {
        return service.update(id, dto);
    }

    @GetMapping("/{id}")
    public TelnyxIpDto getById(@PathVariable String id) {
        return service.getById(id);
    }
}
