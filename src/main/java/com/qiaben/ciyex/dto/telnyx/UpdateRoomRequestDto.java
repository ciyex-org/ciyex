package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class UpdateRoomRequestDto {
    private String uniqueName;
    private Integer maxParticipants;
    private Boolean enableRecording;
    private String webhookEventUrl;
    private String webhookEventFailoverUrl;
    private Integer webhookTimeoutSecs;
}
