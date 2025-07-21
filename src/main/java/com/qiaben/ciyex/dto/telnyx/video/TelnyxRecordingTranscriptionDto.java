package com.qiaben.ciyex.dto.telnyx.video;

import lombok.Data;

@Data
public class TelnyxRecordingTranscriptionDto {
    private String account_sid;
    private String call_sid;
    private String api_version;
    private String date_created;
    private String date_updated;
    private String duration;
    private String sid;
    private String recording_sid;
    private String status;
    private String transcription_text;
    private String uri;
}
