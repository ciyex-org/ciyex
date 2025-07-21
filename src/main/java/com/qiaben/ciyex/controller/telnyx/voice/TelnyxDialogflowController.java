package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxDialogflowRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxDialogflowResponseDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxDialogflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx/dialogflow")
@RequiredArgsConstructor
public class TelnyxDialogflowController {

    private final TelnyxDialogflowService sendTelnyxDialogflowService;

    @PostMapping
    public TelnyxDialogflowResponseDTO sendDialogflow(@RequestBody TelnyxDialogflowRequestDTO dto) {
        return sendTelnyxDialogflowService.createDialogflowConnection(dto);
    }

    @GetMapping("/{connectionId}")
    public TelnyxDialogflowResponseDTO getDialogflow(@PathVariable String connectionId) {
        return sendTelnyxDialogflowService.getDialogflowConnection(connectionId);
    }

    @PutMapping("/{connectionId}")
    public TelnyxDialogflowResponseDTO updateDialogflow(@PathVariable String connectionId, @RequestBody TelnyxDialogflowRequestDTO dto) {
        return sendTelnyxDialogflowService.updateDialogflowConnection(connectionId, dto);
    }
    @DeleteMapping("/{connectionId}")
    public void deleteDialogflow(@PathVariable String connectionId) {
        sendTelnyxDialogflowService.deleteDialogflowConnection(connectionId);
    }


}
