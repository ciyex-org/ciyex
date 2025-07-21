package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxReferCallRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxReferCallResponseDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxReferCallService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/call")
@RequiredArgsConstructor
public class TelnyxReferCallController {

    private final TelnyxReferCallService referCallService;

    @PostMapping("/{callControlId}/refer")
    public ResponseEntity<TelnyxReferCallResponseDTO> referCall(
            @PathVariable String callControlId,
            @RequestBody TelnyxReferCallRequestDTO requestDTO
    ) {
        TelnyxReferCallResponseDTO response = referCallService.referCall(callControlId, requestDTO);
        return ResponseEntity.ok(response);
    }
}
