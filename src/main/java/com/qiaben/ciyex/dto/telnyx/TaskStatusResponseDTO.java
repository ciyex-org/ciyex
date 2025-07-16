package com.qiaben.ciyex.dto.telnyx;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TaskStatusResponseDTO {
    private String taskId;
    private String status;   // pending | processing | completed | failed
    private String createdAt;
    private String updatedAt;
}
