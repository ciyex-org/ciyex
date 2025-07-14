// src/main/java/com/qiaben/ciyex/dto/telnyx/UploadResponseDTO.java
package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class UploadResponseDTO {
    private boolean success;
    private String ticket_id;
}
