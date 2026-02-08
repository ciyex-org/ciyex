package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxMediaDTO {
    private String media_name;
    private String expires_at;
    private String created_at;
    private String updated_at;
    private String content_type;
}