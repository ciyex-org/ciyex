package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TelnyxAutoResponseRequest {
    private String op;
    private List<String> keywords;
    private String respText;
    private String countryCode;
}
