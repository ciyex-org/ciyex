package com.qiaben.ciyex.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminTemplateDto {

    private Long id;
    private String locations;
    private String practiceType;
    private Audit audit;

    @Data
    public static class Audit {
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime updatedAt;
    }
}
