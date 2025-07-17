// src/main/java/com/qiaben/ciyex/dto/telnyx/UploadResponseDTO.java
package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxUploadResponseDTO {
    private boolean success;
    private String ticket_id;
}
