package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxPushCredentialsCreateDto {
    private String type;
    private String certificate;
    private String private_key;
    private String alias;
}
