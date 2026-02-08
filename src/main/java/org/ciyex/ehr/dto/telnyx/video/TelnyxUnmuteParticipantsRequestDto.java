package org.ciyex.ehr.dto.telnyx.video;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxUnmuteParticipantsRequestDto {
    private Object participants;
    private List<String> exclude;
}