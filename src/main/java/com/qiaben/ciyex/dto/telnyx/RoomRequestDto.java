package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class RoomRequestDto {
    private String uniqueName;
    private Integer maxParticipants = 10;  // Default value
    private Boolean enableRecording;
    private String webhookEventUrl;
    private String webhookEventFailoverUrl;
    private Integer webhookTimeoutSecs;


}


