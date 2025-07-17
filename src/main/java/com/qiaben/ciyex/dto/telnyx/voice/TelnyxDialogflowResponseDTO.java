package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxDialogflowResponseDTO {
    private String record_type;
    private String connection_id;
    private String conversation_profile_id;
    private String environment;
    private String service_account;
}
