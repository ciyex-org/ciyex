package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxTeXmlCallRecordingResponseDto {
    private String account_sid;
    private String call_sid;
    private String conference_sid;
    private Integer channels;
    private String date_created;
    private String date_updated;
    private String start_time;
    private String price;
    private String price_unit;
    private String duration;
    private String sid;
    private String source;
    private String error_code;
    private String track;
    private String uri;
}
