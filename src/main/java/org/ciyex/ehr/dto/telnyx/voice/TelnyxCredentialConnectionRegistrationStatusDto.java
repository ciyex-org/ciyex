package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxCredentialConnectionRegistrationStatusDto {
    private String recordType;
    private String status;
    private String sipUsername;
    private String ipAddress;
    private String transport;
    private Integer port;
    private String userAgent;
    private String lastRegistration;
}
