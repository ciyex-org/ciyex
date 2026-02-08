package org.ciyex.ehr.dto.telnyx.voice;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TelnyxTaskStatusResponseDTO {
    private String taskId;
    private String status;   // pending | processing | completed | failed
    private String createdAt;
    private String updatedAt;
}
