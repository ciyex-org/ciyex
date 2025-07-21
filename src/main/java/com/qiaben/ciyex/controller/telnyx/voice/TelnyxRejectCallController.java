package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxRejectCallRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxRejectCallResponseDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxRejectCallService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx")
@RequiredArgsConstructor
public class TelnyxRejectCallController {

    private final TelnyxRejectCallService rejectCallService;

    @PostMapping("/calls/{callControlId}/reject")
    public ResponseEntity<TelnyxRejectCallResponseDTO> rejectCall(
            @PathVariable String callControlId,
            @RequestBody TelnyxRejectCallRequestDTO request) {
        TelnyxRejectCallResponseDTO response = rejectCallService.rejectCall(callControlId, request);
        return ResponseEntity.ok(response);
    }
}
