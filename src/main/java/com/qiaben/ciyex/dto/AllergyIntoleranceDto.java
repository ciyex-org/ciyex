// src/main/java/com/qiaben/ciyex/dto/AllergyIntoleranceDto.java
package com.qiaben.ciyex.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class AllergyIntoleranceDto {

    private String externalId;    // FHIR List id (optional)
    private Long orgId;           // tenant
    private Long patientId;       // EHR patient id
    private List<AllergyItem> allergiesList;
    private Audit audit;

    @Data
    public static class AllergyItem {
        // IMPORTANT: this id is the primary key of the single table row
        private Long id;          // AllergyIntolerance row id (no detail id)
        private String allergyName;
        private String reaction;
        private String severity;
        private String status;
        private Long patientId;
    }

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
