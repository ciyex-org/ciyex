package com.qiaben.ciyex.dto.telnyx.video;

import lombok.Data;

@Data
public class TelnyxRoomRequestDto {
    private String uniqueName;
    private Integer maxParticipants = 10;  // Default value
    private Boolean enableRecording;
    private String webhookEventUrl;
    private String webhookEventFailoverUrl;
    private Integer webhookTimeoutSecs;


}


