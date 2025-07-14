package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.TelnyxUpdateClientStateRequestDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxUpdateClientStateResponseDTO;
import com.qiaben.ciyex.service.telnyx.TelnyxUpdateClientStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/call")
@RequiredArgsConstructor
public class TelnyxUpdateClientStateController {

    private final TelnyxUpdateClientStateService updateClientStateService;

    @PutMapping("/{callControlId}/action/client_state_update")
    public TelnyxUpdateClientStateResponseDTO updateClientState(
            @PathVariable String callControlId,
            @RequestBody TelnyxUpdateClientStateRequestDTO requestDTO) {
        return updateClientStateService.updateClientState(callControlId, requestDTO);
    }
}
