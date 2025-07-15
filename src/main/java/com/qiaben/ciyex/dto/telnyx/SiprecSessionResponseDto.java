package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class SiprecSessionResponseDto {
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

