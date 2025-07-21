package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxMediaUploadRequest {
    private String media_url;
    private Integer ttl_secs;
    private String media_name;
}