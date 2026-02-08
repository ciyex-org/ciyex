package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxUnholdConferenceParticipantsRequestDto {
    private List<String> callControlIds;
}
