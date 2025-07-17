package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxEnqueueRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxEnqueueResponseDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxEnqueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/enqueue")
@RequiredArgsConstructor
public class TelnyxEnqueueController {

    private final TelnyxEnqueueService enqueueService;

    @PostMapping("/{callControlId}")
    public ResponseEntity<TelnyxEnqueueResponseDTO> enqueue(
            @PathVariable String callControlId,
            @RequestBody TelnyxEnqueueRequestDTO body
    ) {
        return ResponseEntity.ok(enqueueService.enqueueCall(callControlId, body));
    }
}
