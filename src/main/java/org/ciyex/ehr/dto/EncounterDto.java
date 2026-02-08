

package org.ciyex.ehr.dto;

import java.time.LocalDateTime;

import org.ciyex.ehr.dto.EncounterStatus;

import lombok.Data;

@Data
public class EncounterDto {

    private Long id;
    private Long patientId;
    private String visitCategory;
    private String encounterProvider;
    private String type;
    private String sensitivity;
    private String dischargeDisposition;
    private String reasonForVisit;
    private Boolean inCollection;
    private LocalDateTime encounterDate;

    private EncounterStatus status;
    private String externalId;
    private String fhirId;
    private String diagnosis; // Diagnosis codes from procedures

    // audit
    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;      // yyyy-MM-dd
        private String lastModifiedDate; // yyyy-MM-dd
    }

}