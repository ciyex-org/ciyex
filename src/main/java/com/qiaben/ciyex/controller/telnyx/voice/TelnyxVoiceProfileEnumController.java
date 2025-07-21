package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxVoiceProfileEnumDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxVoiceProfileEnumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/voice-profiles/enums")
@RequiredArgsConstructor
public class TelnyxVoiceProfileEnumController {

    private final TelnyxVoiceProfileEnumService service;

    @GetMapping("/{endpoint}")
    public ResponseEntity<TelnyxVoiceProfileEnumDto> getEnum(@PathVariable String endpoint) {
        TelnyxVoiceProfileEnumDto response = service.getEnumValues(endpoint);
        return ResponseEntity.ok(response);
    }
}
