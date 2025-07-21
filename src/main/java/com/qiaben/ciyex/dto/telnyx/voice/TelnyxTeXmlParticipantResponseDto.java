package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxTeXmlParticipantResponseDto {
    private String account_sid;
    private String api_version;
    private String call_sid;
    private String call_sid_legacy;
    private boolean coaching;
    private String coaching_call_sid;
    private String coaching_call_sid_legacy;
    private String date_created;
    private String date_updated;
    private boolean end_conference_on_exit;
    private boolean hold;
    private boolean muted;
    private String status;
    private String uri;
}
