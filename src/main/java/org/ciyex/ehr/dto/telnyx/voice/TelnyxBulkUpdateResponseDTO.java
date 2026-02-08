
// src/main/java/com/ciyex/ciyex/dto/telnyx/BulkUpdateResponseDTO.java
package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxBulkUpdateResponseDTO {
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

