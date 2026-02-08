package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxUpdateConferenceParticipantRequestDto {
    private String callControlId;
    private String commandId;
    private String supervisorRole; // barge | monitor | none | whisper
    private List<String> whisperCallControlIds;
}
