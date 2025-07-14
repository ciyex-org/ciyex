package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.IpConnectionCreateDto;
import com.qiaben.ciyex.dto.telnyx.IpConnectionUpdateDto;
import com.qiaben.ciyex.dto.telnyx.IpConnectionResponseDto;
import com.qiaben.ciyex.service.telnyx.IpConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/ip-connections")
@RequiredArgsConstructor
public class IpConnectionController {

    private final IpConnectionService service;

    @GetMapping
    public IpConnectionResponseDto getIpConnections(
            @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "connectionNameContains", required = false) String connectionNameContains,
            @RequestParam(value = "outboundVoiceProfileId", required = false) Long outboundVoiceProfileId,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        return service.listIpConnections(pageNumber, pageSize, connectionNameContains, outboundVoiceProfileId, sort);
    }

    @GetMapping("/{id}")
    public IpConnectionResponseDto.IpConnectionData getIpConnectionById(@PathVariable String id) {
        return service.getIpConnectionById(id);
    }

    @PostMapping
    public IpConnectionResponseDto.IpConnectionData createIpConnection(
            @RequestBody IpConnectionCreateDto request
    ) {
        return service.createIpConnection(request);
    }

    @PatchMapping("/{id}")
    public IpConnectionResponseDto.IpConnectionData updateIpConnection(
            @PathVariable String id,
            @RequestBody IpConnectionUpdateDto request
    ) {
        return service.updateIpConnection(id, request);
    }

    @DeleteMapping("/{id}")
    public IpConnectionResponseDto.IpConnectionData deleteIpConnection(@PathVariable String id) {
        return service.deleteIpConnection(id);
    }
}
