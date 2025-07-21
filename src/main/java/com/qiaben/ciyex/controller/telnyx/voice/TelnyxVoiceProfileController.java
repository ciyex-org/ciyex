package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateVoiceProfileRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateVoiceProfileResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxVoiceProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/voice-profile")
@RequiredArgsConstructor
public class TelnyxVoiceProfileController {

    private final TelnyxVoiceProfileService service;

    @PatchMapping("/{brandId}")
    public TelnyxUpdateVoiceProfileResponseDto updateVoiceProfile(
            @PathVariable String brandId,
            @RequestBody TelnyxUpdateVoiceProfileRequestDto request) {
        return service.updateVoiceProfile(brandId, request);
    }
}
