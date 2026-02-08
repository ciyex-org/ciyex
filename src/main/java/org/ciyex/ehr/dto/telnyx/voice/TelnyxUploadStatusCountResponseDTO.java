// src/main/java/com/ciyex/ciyex/dto/telnyx/UploadStatusCountResponseDTO.java
package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxUploadStatusCountResponseDTO {
    private Data data;

    @lombok.Data
    public static class Data {
        private Integer pending_numbers_count;
        private Integer pending_orders_count;
    }
}
