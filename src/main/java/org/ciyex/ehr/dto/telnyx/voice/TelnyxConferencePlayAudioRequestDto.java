package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxConferencePlayAudioRequestDto {
    private String audioUrl;               // Optional, mutually exclusive with mediaName
    private String mediaName;              // Optional, mutually exclusive with audioUrl
    private Object loop;                   // Integer (1-100) or "infinity"
    private List<String> callControlIds;   // Optional: target specific participants
}
