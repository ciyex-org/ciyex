package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class ConferenceAudioStopRequestDto {
    private List<String> callControlIds;  // Optional: stop audio for specific participants
}
