package com.qiaben.ciyex.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TemplateDto {
    private Long id;
    private String externalId;
    private String templateName;
    private String subject;
    private String body;
    private Audit audit;

    @Data
    public static class Audit {
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
