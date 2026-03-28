package org.ciyex.ehr.recall.dto;

import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RecallTypeDto {
    private Long id;
    private String name;
    private String code;
    private String category;
    private Integer intervalMonths;
    private Integer intervalDays;
    private Integer leadTimeDays;
    private Integer maxAttempts;
    private String priority;
    private Boolean autoCreate;
    private List<String> communicationSequence;
    private List<Integer> escalationWaitDays;
    private String appointmentTypeCode;
    private Boolean active;
    private String createdBy;
    private String createdAt;
    private String updatedAt;
}
