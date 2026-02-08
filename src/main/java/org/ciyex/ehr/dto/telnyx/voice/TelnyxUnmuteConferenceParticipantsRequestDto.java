package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxUnmuteConferenceParticipantsRequestDto {
    private List<String> callControlIds; // Optional: unmute all if null or empty
}
