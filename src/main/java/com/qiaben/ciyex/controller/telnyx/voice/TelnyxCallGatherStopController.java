package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxGatherStopRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxGatherStopResponseDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxCallGatherStopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/calls")
@RequiredArgsConstructor
public class TelnyxCallGatherStopController {

    private final TelnyxCallGatherStopService stopService;

    @PostMapping("/{callControlId}/gather-stop")
    public ResponseEntity<TelnyxGatherStopResponseDTO> stopGather(
            @PathVariable String callControlId,
            @RequestBody TelnyxGatherStopRequestDTO requestDTO) {
        TelnyxGatherStopResponseDTO response = stopService.stopGather(callControlId, requestDTO);
        return ResponseEntity.ok(response);
    }
}
