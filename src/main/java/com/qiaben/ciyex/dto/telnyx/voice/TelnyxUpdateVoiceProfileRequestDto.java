package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxUpdateVoiceProfileRequestDto {
    private String entityType;
    private String displayName;
    private String companyName;
    private String firstName;
    private String lastName;
    private String ein;
    private String phone;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String email;
    private String stockSymbol;
    private String stockExchange;
    private String ipAddress;
    private String website;
    private String vertical;
    private String altBusinessId;
    private String altBusinessIdType;
    private Boolean isReseller;
    private String identityStatus;
    private String businessContactEmail;
    private String webhookURL;
    private String webhookFailoverURL;
}
