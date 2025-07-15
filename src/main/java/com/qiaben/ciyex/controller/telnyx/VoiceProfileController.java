package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.UpdateVoiceProfileRequestDto;
import com.qiaben.ciyex.dto.telnyx.UpdateVoiceProfileResponseDto;
import com.qiaben.ciyex.service.telnyx.VoiceProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/voice-profile")
@RequiredArgsConstructor
public class VoiceProfileController {

    private final VoiceProfileService service;

    @PatchMapping("/{brandId}")
    public UpdateVoiceProfileResponseDto updateVoiceProfile(
            @PathVariable String brandId,
            @RequestBody UpdateVoiceProfileRequestDto request) {
        return service.updateVoiceProfile(brandId, request);
    }
}
