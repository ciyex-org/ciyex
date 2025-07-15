package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class CredentialConnectionResponseDto {
    private Long id;
    private String recordType;
    private Boolean active;
    private String userName;
    private String password;
    private String createdAt;
    private String updatedAt;
    private String anchorsiteOverride;
    private String connectionName;
    private String sipUriCallingPreference;
    private Boolean defaultOnHoldComfortNoiseEnabled;
    private String dtmfType;
    private Boolean encodeContactHeaderEnabled;
    private String encryptedMedia;
    private Boolean onnetT38PassthroughEnabled;
    private String webhookEventUrl;
    private String webhookEventFailoverUrl;
    private String webhookApiVersion;
    private Integer webhookTimeoutSecs;
    private String[] tags;
}
