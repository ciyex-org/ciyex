package org.ciyex.ehr.kiosk.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class KioskConfigDto {
    private Long id;
    private Boolean enabled;
    private String config;           // JSON string
    private String welcomeMessage;
    private String completionMessage;
    private Integer idleTimeoutSec;
    private String createdAt;
    private String updatedAt;
}
