package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxHangupCallRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxHangupCallResponseDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxHangupCallService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/calls")
@RequiredArgsConstructor
public class TelnyxHangupCallController {

    private final TelnyxHangupCallService hangupCallService;

    @PostMapping("/{callControlId}/hangup")
    public ResponseEntity<TelnyxHangupCallResponseDTO> hangupCall(
            @PathVariable String callControlId,
            @RequestBody TelnyxHangupCallRequestDTO requestDTO) {
        TelnyxHangupCallResponseDTO response = hangupCallService.hangupCall(callControlId, requestDTO);
        return ResponseEntity.ok(response);
    }
}
