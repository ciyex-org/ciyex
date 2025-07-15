package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class BulkUpdateResponseDTO {
    private Bulk data;

    @Data
    public static class Bulk {
        private String recordType;
        private String orderId;
        private List<String> success;
        private List<String> pending;
        private List<String> failed;
    }
}

