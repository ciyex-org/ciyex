package org.ciyex.ehr.careplan.dto;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CarePlanGoalDto {
    private Long id;
    private Long carePlanId;
    private String description;
    private String targetDate;
    private String status;
    private String measure;
    private String currentValue;
    private String targetValue;
    private String priority;
    private String notes;
    private String createdAt;
    private String updatedAt;
    private List<CarePlanInterventionDto> interventions;
}
