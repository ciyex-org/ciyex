package com.qiaben.ciyex.service.telnyx;


import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateVoiceProfileRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateVoiceProfileResponseDto;

public interface VoiceProfileService {
    TelnyxUpdateVoiceProfileResponseDto updateVoiceProfile(String brandId, TelnyxUpdateVoiceProfileRequestDto request);
}
