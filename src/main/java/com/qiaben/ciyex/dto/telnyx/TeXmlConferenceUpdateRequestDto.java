package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class TeXmlConferenceUpdateRequestDto {
    private String Status;          // Possible value: completed
    private String AnnounceUrl;     // URL to play announcement
    private String AnnounceMethod;  // GET or POST
}
