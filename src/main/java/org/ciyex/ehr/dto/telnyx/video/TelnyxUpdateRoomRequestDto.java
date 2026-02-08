package org.ciyex.ehr.dto.telnyx.video;

import lombok.Data;

@Data
public class TelnyxUpdateRoomRequestDto {
    private String uniqueName;
    private Integer maxParticipants;
    private Boolean enableRecording;
    private String webhookEventUrl;
    private String webhookEventFailoverUrl;
    private Integer webhookTimeoutSecs;
}
