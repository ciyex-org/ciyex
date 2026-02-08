package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxKickParticipantsRequestDto {
    private Object participants;
    private List<String> exclude;
}
