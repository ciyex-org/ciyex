package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxCredentialsDto {
    // Request fields
    private String name;
    private String tag;
    private String connection_id;
    private String expires_at;

    // Response fields
    private String id;
    private String record_type;
    private String resource_id;
    private boolean expired;
    private String sip_username;
    private String sip_password;
    private String created_at;
    private String updated_at;
}
