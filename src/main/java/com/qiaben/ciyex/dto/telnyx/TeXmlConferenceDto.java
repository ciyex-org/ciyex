package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.Map;

@Data
public class TeXmlConferenceDto {
    private String account_sid;
    private String api_version;
    private String call_sid_ending_conference;
    private String date_created;
    private String date_updated;
    private String friendly_name;
    private String reason_conference_ended;
    private String region;
    private String sid;
    private String status;
    private Map<String, String> subresource_uris;
    private String uri;
}
