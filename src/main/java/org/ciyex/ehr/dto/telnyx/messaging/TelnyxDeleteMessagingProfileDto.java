package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TelnyxDeleteMessagingProfileDto {
    private String id;
    private String message;
}
