package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConnectionResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/connections")
@RequiredArgsConstructor
public class TelnyxConnectionController {

    private final TelnyxConnectionService service;

    @GetMapping
    public TelnyxConnectionResponseDto getConnections(
            @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "connectionNameContains", required = false) String connectionNameContains,
            @RequestParam(value = "outboundVoiceProfileId", required = false) Long outboundVoiceProfileId,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        return service.listConnections(pageNumber, pageSize, connectionNameContains, outboundVoiceProfileId, sort);
    }

    @GetMapping("/{id}")
    public TelnyxConnectionResponseDto.ConnectionData getConnectionById(@PathVariable String id) {
        return service.getConnectionById(id);
    }
}
