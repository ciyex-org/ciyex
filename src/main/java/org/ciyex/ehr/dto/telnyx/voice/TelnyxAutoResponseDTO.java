package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class TelnyxAutoResponseDTO {
    private String op;
    private List<String> keywords;
    private String respText;
    private String countryCode;
    private String id;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
