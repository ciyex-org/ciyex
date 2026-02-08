package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxTeXmlConferenceUpdateRequestDto {
    private String Status;          // Possible value: completed
    private String AnnounceUrl;     // URL to play announcement
    private String AnnounceMethod;  // GET or POST
}
