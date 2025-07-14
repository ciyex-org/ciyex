package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ConnectionResponseDto;
import com.qiaben.ciyex.service.telnyx.ConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService service;

    @GetMapping
    public ConnectionResponseDto getConnections(
            @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "connectionNameContains", required = false) String connectionNameContains,
            @RequestParam(value = "outboundVoiceProfileId", required = false) Long outboundVoiceProfileId,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        return service.listConnections(pageNumber, pageSize, connectionNameContains, outboundVoiceProfileId, sort);
    }

    @GetMapping("/{id}")
    public ConnectionResponseDto.ConnectionData getConnectionById(@PathVariable String id) {
        return service.getConnectionById(id);
    }
}
