package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.List;

@Data
public class UpdateConferenceParticipantRequestDto {
    private String callControlId;
    private String commandId;
    private String supervisorRole; // barge | monitor | none | whisper
    private List<String> whisperCallControlIds;
}
