
// src/main/java/com/qiaben/ciyex/dto/telnyx/BulkUpdateRequestDTO.java
package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxBulkUpdateRequestDTO {

    private String messagingProfileId;     // "" to un‑assign
    private List<String> numbers;          // +E.164 list
}
