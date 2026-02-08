package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxVerifiedNumberDTO {
    private String phone_number;
    private String record_type;
    private String verified_at;
}
