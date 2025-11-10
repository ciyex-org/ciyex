package com.qiaben.ciyex.dto.portal;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class PortalHistoryDto {

    private Long patientId;
    private List<HistoryItem> history;

    @Data
    public static class HistoryItem {
        private Long id;
        private String medicalCondition;
        private String conditionName;
        private String status;
        private Boolean isChronic;
        private LocalDateTime diagnosisDate;
        private LocalDate onsetDate;
        private LocalDate resolvedDate;
        private String treatmentDetails;
        private String diagnosisDetails;
        private String notes;
        private String description;
    }

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}