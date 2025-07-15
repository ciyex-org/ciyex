package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.RcsAgentDTO;
import com.qiaben.ciyex.dto.telnyx.RcsAgentListResponse;
import com.qiaben.ciyex.dto.telnyx.RcsAgentUpdateRequest;
import com.qiaben.ciyex.service.telnyx.TelnyxRcsAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/rcs/agents")
@RequiredArgsConstructor
public class TelnyxRcsAgentController {

    private final TelnyxRcsAgentService agentService;

    @GetMapping
    public ResponseEntity<RcsAgentListResponse> listAgents(
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.ok(agentService.listAgents(pageNumber, pageSize));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RcsAgentDTO> getAgent(@PathVariable String id) {
        return ResponseEntity.ok(agentService.getAgentById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RcsAgentDTO> updateAgent(
            @PathVariable String id,
            @RequestBody RcsAgentUpdateRequest request
    ) {
        return ResponseEntity.ok(agentService.updateAgent(id, request));
    }
}
