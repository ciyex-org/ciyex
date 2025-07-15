// src/main/java/com/qiaben/ciyex/dto/telnyx/BulkUpdateRequestDTO.java
package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class BulkUpdateRequestDTO {
    private String messagingProfileId;     // "" to un‑assign
    private List<String> numbers;          // +E.164 list
}
