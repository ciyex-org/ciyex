package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.Map;

@Data
public class TeXmlRecordingDto {
    private String account_sid;
    private String call_sid;
    private Integer channels;
    private String conference_sid;
    private String date_created;
    private String date_updated;
    private Integer duration;
    private String error_code;
    private String media_url;
    private String sid;
    private String source;
    private String start_time;
    private String status;
    private Map<String, String> subresource_uris;
    private String uri;
}
