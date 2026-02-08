package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxMuteConferenceParticipantsRequestDto {
    private List<String> callControlIds; // Optional: mute all if null or empty
}
