package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;

@Data
public class TelnyxPhoneNumberRequestDTO {
    private String location_id; // Required in PATCH request body
}
