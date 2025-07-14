package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.IpDto;
import com.qiaben.ciyex.dto.telnyx.IpListResponseDto;
import com.qiaben.ciyex.service.telnyx.IpService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/ips")
@RequiredArgsConstructor
public class IpController {

    private final IpService service;

    @GetMapping
    public IpListResponseDto list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "250") int size,
            @RequestParam(required = false, name = "connection_id") String connectionId,
            @RequestParam(required = false, name = "ip_address") String ipAddress,
            @RequestParam(required = false) Integer port
    ) {
        return service.list(page, size, connectionId, ipAddress, port);
    }

    @PostMapping
    public IpDto create(@RequestBody IpDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public IpDto update(@PathVariable String id, @RequestBody IpDto dto) {
        return service.update(id, dto);
    }

    @GetMapping("/{id}")
    public IpDto getById(@PathVariable String id) {
        return service.getById(id);
    }
}
