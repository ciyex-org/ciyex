package org.ciyex.ehr.dto.portal;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class PortalAllergyDto {

    private Long patientId;
    private List<AllergyItem> allergies;

    @Data
    public static class AllergyItem {
        private Long id;
        private String allergyName;
        private String reaction;
        private String severity;
        private String status;
        private String startDate;
        private String endDate;
        private String comments;
    }

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}