package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class StartStreamRequestDto {
    private String statusCallback;
    private String statusCallbackMethod;
    private String track;
    private String name;
    private String bidirectionalMode;
    private String bidirectionalCodec;
    private String url;
}

