
// src/main/java/com/ciyex/ciyex/dto/telnyx/BulkUpdateRequestDTO.java
package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxBulkUpdateRequestDTO {

    private String messagingProfileId;     // "" to un‑assign
    private List<String> numbers;          // +E.164 list
}
