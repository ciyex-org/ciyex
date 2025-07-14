package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.FqdnConnectionDto;
import com.qiaben.ciyex.dto.telnyx.FqdnConnectionListResponseDto;
import com.qiaben.ciyex.service.telnyx.FqdnConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/fqdn-connections")
@RequiredArgsConstructor
public class FqdnConnectionController {

    private final FqdnConnectionService service;

    @GetMapping
    public FqdnConnectionListResponseDto list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "250") int size,
            @RequestParam(required = false) String connectionId,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) Integer port
    ) {
        return service.list(connectionId, ipAddress, port, page, size);
    }

    @PostMapping
    public FqdnConnectionDto create(@RequestBody FqdnConnectionDto dto) {
        return service.create(dto);
    }

    @PatchMapping("/{id}")
    public FqdnConnectionDto update(@PathVariable String id, @RequestBody FqdnConnectionDto dto) {
        return service.update(id, dto);
    }

    @GetMapping("/{id}")
    public FqdnConnectionDto get(@PathVariable String id) {
        return service.getById(id);
    }
}
