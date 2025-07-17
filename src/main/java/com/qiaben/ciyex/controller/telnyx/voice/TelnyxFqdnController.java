package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxFqdnListResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxFqdnRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxFqdnResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxFqdnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/fqdns")
@RequiredArgsConstructor
public class TelnyxFqdnController {

    private final TelnyxFqdnService telnyxFqdnService;

    @GetMapping
    public TelnyxFqdnListResponseDto list(
            @RequestParam(defaultValue = "1", name = "page[number]") Integer page,
            @RequestParam(defaultValue = "250", name = "page[size]") Integer size,
            @RequestParam(value = "filter[connection_id]", required = false) String connectionId,
            @RequestParam(value = "filter[fqdn]", required = false) String fqdn,
            @RequestParam(value = "filter[port]", required = false) Integer port,
            @RequestParam(value = "filter[dns_record_type]", required = false) String dnsRecordType) {

        return telnyxFqdnService.list(page, size, connectionId, fqdn, port, dnsRecordType);
    }

    @PostMapping
    public TelnyxFqdnResponseDto create(@RequestBody TelnyxFqdnRequestDto req) {
        return telnyxFqdnService.create(req);
    }

    @GetMapping("/{id}")
    public TelnyxFqdnResponseDto get(@PathVariable String id) {
        return telnyxFqdnService.getById(id);
    }

    @PatchMapping("/{id}")
    public TelnyxFqdnResponseDto update(@PathVariable String id, @RequestBody TelnyxFqdnRequestDto req) {
        return telnyxFqdnService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public TelnyxFqdnResponseDto delete(@PathVariable String id) {
        return telnyxFqdnService.delete(id);
    }
}
