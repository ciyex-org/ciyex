package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.VoiceProfileEnumDto;
import com.qiaben.ciyex.service.telnyx.VoiceProfileEnumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/voice-profiles/enums")
@RequiredArgsConstructor
public class VoiceProfileEnumController {

    private final VoiceProfileEnumService service;

    @GetMapping("/{endpoint}")
    public ResponseEntity<VoiceProfileEnumDto> getEnum(@PathVariable String endpoint) {
        VoiceProfileEnumDto response = service.getEnumValues(endpoint);
        return ResponseEntity.ok(response);
    }
}
