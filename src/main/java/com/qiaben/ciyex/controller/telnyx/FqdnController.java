package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.*;
import com.qiaben.ciyex.service.telnyx.FqdnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/fqdns")
@RequiredArgsConstructor
public class FqdnController {

    private final FqdnService fqdnService;

    @GetMapping
    public FqdnListResponseDto list(
            @RequestParam(defaultValue = "1", name = "page[number]") Integer page,
            @RequestParam(defaultValue = "250", name = "page[size]") Integer size,
            @RequestParam(value = "filter[connection_id]", required = false) String connectionId,
            @RequestParam(value = "filter[fqdn]", required = false) String fqdn,
            @RequestParam(value = "filter[port]", required = false) Integer port,
            @RequestParam(value = "filter[dns_record_type]", required = false) String dnsRecordType) {

        return fqdnService.list(page, size, connectionId, fqdn, port, dnsRecordType);
    }

    @PostMapping
    public FqdnResponseDto create(@RequestBody FqdnRequestDto req) {
        return fqdnService.create(req);
    }

    @GetMapping("/{id}")
    public FqdnResponseDto get(@PathVariable String id) {
        return fqdnService.getById(id);
    }

    @PatchMapping("/{id}")
    public FqdnResponseDto update(@PathVariable String id, @RequestBody FqdnRequestDto req) {
        return fqdnService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public FqdnResponseDto delete(@PathVariable String id) {
        return fqdnService.delete(id);
    }
}
