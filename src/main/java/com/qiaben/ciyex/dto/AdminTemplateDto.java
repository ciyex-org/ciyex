package com.qiaben.ciyex.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminTemplateDto {

    private Long id;
    private Long orgId;
    private String templateId;
    private String locations;
    private String practiceType;
    private Audit audit;

    @Data
    public static class Audit {
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
