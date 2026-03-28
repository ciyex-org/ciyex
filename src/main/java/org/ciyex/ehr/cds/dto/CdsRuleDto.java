package org.ciyex.ehr.cds.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CdsRuleDto {
    private Long id;
    private String name;
    private String description;
    private String ruleType;
    private String category;
    private String triggerEvent;
    private Object conditions;   // JSON object (accepts any JSON structure from frontend)
    private String actionType;
    private String severity;
    private String message;
    private String recommendation;
    private String referenceUrl;
    private Boolean isActive;
    private String appliesTo;
    private Integer snoozeDays;
    private String createdAt;
    private String updatedAt;
}
