// src/main/java/com/ciyex/ciyex/dto/telnyx/UploadResponseDTO.java
package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxUploadResponseDTO {
    private boolean success;
    private String ticket_id;
}
