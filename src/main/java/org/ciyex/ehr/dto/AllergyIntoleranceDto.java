package org.ciyex.ehr.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class AllergyIntoleranceDto {

    private String fhirId;
    private String externalId;
    private Long patientId;
    private List<AllergyItem> allergiesList;
    private Audit audit;

    @Data
    public static class AllergyItem {
        private Long id;
        private String fhirId;
        private String externalId;
        
        private String allergyName;
        private String reaction;
        private String severity;
        private String status;
        private Long patientId;

        private String startDate;
        private String endDate;

        private String comments;
        private Audit audit;
    }

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
