package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxSiprecSessionResponseDto {
    private String account_sid;
    private String call_sid;
    private String sid;
    private String date_created;
    private String date_updated;
    private String start_time;
    private String status;
    private String track;
    private String uri;
    private String error_code;
}

