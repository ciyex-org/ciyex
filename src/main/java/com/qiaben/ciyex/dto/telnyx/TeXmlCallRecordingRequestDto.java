package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class TeXmlCallRecordingRequestDto {
    private Boolean PlayBeep;                         // default true
    private String  RecordingStatusCallbackEvent;     // "in-progress completed"
    private String  RecordingStatusCallback;          // URL
    private String  RecordingStatusCallbackMethod;    // GET | POST
    private String  RecordingChannels;                // single | dual
    private String  RecordingTrack;                   // inbound | outbound | both
    private Boolean SendRecordingUrl;                 // default true
}
