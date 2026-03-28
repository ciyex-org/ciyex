package org.ciyex.ehr.careplan.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CarePlanInterventionDto {
    private Long id;
    private Long carePlanId;
    private Long goalId;
    private String description;
    private String assignedTo;
    private String frequency;
    private String status;
    private String notes;
    private String createdAt;
    private String updatedAt;
}
