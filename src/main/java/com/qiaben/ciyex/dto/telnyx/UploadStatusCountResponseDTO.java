// src/main/java/com/qiaben/ciyex/dto/telnyx/UploadStatusCountResponseDTO.java
package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class UploadStatusCountResponseDTO {
    private Data data;

    @lombok.Data
    public static class Data {
        private Integer pending_numbers_count;
        private Integer pending_orders_count;
    }
}
