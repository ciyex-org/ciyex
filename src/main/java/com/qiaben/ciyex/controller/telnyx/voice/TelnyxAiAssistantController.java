package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxAiAssistantRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxAiAssistantResponseDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxAiAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/calls")
@RequiredArgsConstructor
public class TelnyxAiAssistantController {

    private final TelnyxAiAssistantService aiAssistantService;

    @PostMapping("/{callControlId}/ai-assistant-start")
    public ResponseEntity<TelnyxAiAssistantResponseDTO> startAi(@PathVariable String callControlId,
                                                                @RequestBody TelnyxAiAssistantRequestDTO requestDTO) {
        TelnyxAiAssistantResponseDTO response = aiAssistantService.startAi(callControlId, requestDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{callControlId}/ai-assistant-stop")
    public ResponseEntity<TelnyxAiAssistantResponseDTO> stopAi(@PathVariable String callControlId,
                                                               @RequestBody TelnyxAiAssistantRequestDTO requestDTO) {
        TelnyxAiAssistantResponseDTO response = aiAssistantService.stopAi(callControlId, requestDTO);
        return ResponseEntity.ok(response);
    }
}
