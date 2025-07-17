package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxConferenceAudioStopRequestDto {
    private List<String> callControlIds;  // Optional: stop audio for specific participants
}
