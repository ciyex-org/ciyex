package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateClientStateRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateClientStateResponseDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxUpdateClientStateService;
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
