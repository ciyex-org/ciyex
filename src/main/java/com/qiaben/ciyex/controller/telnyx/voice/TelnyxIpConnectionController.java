package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxIpConnectionCreateDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxIpConnectionUpdateDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxIpConnectionResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxIpConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/ip-connections")
@RequiredArgsConstructor
public class TelnyxIpConnectionController {

    private final TelnyxIpConnectionService service;

    @GetMapping
    public TelnyxIpConnectionResponseDto getIpConnections(
            @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "connectionNameContains", required = false) String connectionNameContains,
            @RequestParam(value = "outboundVoiceProfileId", required = false) Long outboundVoiceProfileId,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        return service.listIpConnections(pageNumber, pageSize, connectionNameContains, outboundVoiceProfileId, sort);
    }

    @GetMapping("/{id}")
    public TelnyxIpConnectionResponseDto.IpConnectionData getIpConnectionById(@PathVariable String id) {
        return service.getIpConnectionById(id);
    }

    @PostMapping
    public TelnyxIpConnectionResponseDto.IpConnectionData createIpConnection(
            @RequestBody TelnyxIpConnectionCreateDto request
    ) {
        return service.createIpConnection(request);
    }

    @PatchMapping("/{id}")
    public TelnyxIpConnectionResponseDto.IpConnectionData updateIpConnection(
            @PathVariable String id,
            @RequestBody TelnyxIpConnectionUpdateDto request
    ) {
        return service.updateIpConnection(id, request);
    }

    @DeleteMapping("/{id}")
    public TelnyxIpConnectionResponseDto.IpConnectionData deleteIpConnection(@PathVariable String id) {
        return service.deleteIpConnection(id);
    }
}
