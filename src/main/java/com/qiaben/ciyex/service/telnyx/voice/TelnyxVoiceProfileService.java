package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateVoiceProfileRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateVoiceProfileResponseDto;

public interface TelnyxVoiceProfileService {
    TelnyxUpdateVoiceProfileResponseDto updateVoiceProfile(String brandId, TelnyxUpdateVoiceProfileRequestDto request);
}
