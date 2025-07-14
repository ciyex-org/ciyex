package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.TelnyxReferCallRequestDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxReferCallResponseDTO;
import com.qiaben.ciyex.service.telnyx.TelnyxReferCallService;
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
