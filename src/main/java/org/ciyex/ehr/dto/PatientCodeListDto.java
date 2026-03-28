package org.ciyex.ehr.dto;

public class PatientCodeListDto {
    public Long id;     // echoed for completeness
    public String title;
    public Integer order;  // maps to entity.orderIndex
    public boolean isDefault;
    public boolean active;
    public String notes;
    public String codes;
    public String externalId;
    public String fhirId;

    // audit
    public Audit audit;

    public static class Audit {
        public String createdDate;      // yyyy-MM-dd
        public String lastModifiedDate; // yyyy-MM-dd
    }
}