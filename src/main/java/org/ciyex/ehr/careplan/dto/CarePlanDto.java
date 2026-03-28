package org.ciyex.ehr.careplan.dto;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CarePlanDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private String title;
    private String status;
    private String category;
    private String startDate;
    private String endDate;
    private String authorName;
    private String description;
    private String notes;
    private String createdAt;
    private String updatedAt;
    private List<CarePlanGoalDto> goals;
    private List<CarePlanInterventionDto> interventions;
}
