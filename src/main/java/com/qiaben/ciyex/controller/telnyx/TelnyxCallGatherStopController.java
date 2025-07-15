package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.TelnyxGatherStopRequestDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxGatherStopResponseDTO;
import com.qiaben.ciyex.service.telnyx.TelnyxCallGatherStopService;
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
