package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxRcsAgentDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxRcsAgentListResponse;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxRcsAgentUpdateRequest;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxRcsAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/rcs/agents")
@RequiredArgsConstructor
public class TelnyxRcsAgentController {

    private final TelnyxRcsAgentService agentService;

    @GetMapping
    public ResponseEntity<TelnyxRcsAgentListResponse> listAgents(
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.ok(agentService.listAgents(pageNumber, pageSize));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TelnyxRcsAgentDTO> getAgent(@PathVariable String id) {
        return ResponseEntity.ok(agentService.getAgentById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TelnyxRcsAgentDTO> updateAgent(
            @PathVariable String id,
            @RequestBody TelnyxRcsAgentUpdateRequest request
    ) {
        return ResponseEntity.ok(agentService.updateAgent(id, request));
    }
}
