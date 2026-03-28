package org.ciyex.ehr.education.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EducationMaterialDto {
    private Long id;
    private String title;
    private String category;
    private String contentType;
    private String content;
    private String externalUrl;
    private String language;
    private String audience;
    private String tags;
    private String author;
    private String source;
    private Boolean isActive;
    private Integer viewCount;
    private String createdAt;
    private String updatedAt;
}
