package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.dto.telnyx.UpdateVoiceProfileRequestDto;
import com.qiaben.ciyex.dto.telnyx.UpdateVoiceProfileResponseDto;

public interface VoiceProfileService {
    UpdateVoiceProfileResponseDto updateVoiceProfile(String brandId, UpdateVoiceProfileRequestDto request);
}
