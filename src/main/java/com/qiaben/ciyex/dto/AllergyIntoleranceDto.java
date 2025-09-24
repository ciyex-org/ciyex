package com.qiaben.ciyex.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class AllergyIntoleranceDto {

    private String externalId;
    private Long orgId;           // tenant
    private Long patientId;       // EHR patient id (omitted in API responses)
    private List<AllergyItem> allergiesList;
    private Audit audit;

    @Data
    public static class AllergyItem {
        private Long id;          // primary key row id
        private String allergyName;
        private String reaction;
        private String severity;
        private String status;
        private Long patientId;

        // Effective window
        private String startDate;  // ISO yyyy-MM-dd preferred
        private String endDate;    // ISO yyyy-MM-dd preferred

        // NEW
        private String comments;
    }

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
