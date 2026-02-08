package org.ciyex.ehr.dto.integration;

import lombok.Data;

@Data
public class TwilioConfig {
    private String accountSid;
    private String authToken;
    private String phoneNumber;
}
