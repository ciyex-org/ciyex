package org.ciyex.ehr.recall.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RecallOutreachLogDto {
    private Long id;
    private Long recallId;
    private Integer attemptNumber;
    private String attemptDate;
    private String method;
    private String direction;
    private String performedBy;
    private String performedByName;
    private String outcome;
    private String notes;
    private String nextAction;
    private String nextActionDate;
    private Boolean automated;
    private String deliveryStatus;
    private String createdAt;
}
