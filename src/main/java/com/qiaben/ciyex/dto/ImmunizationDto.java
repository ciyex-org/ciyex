package com.qiaben.ciyex.dto;

import lombok.Data;
import java.util.List;

@Data
public class ImmunizationDto {
    private Long patientId;
    private List<ImmunizationItem> immunizations;

    @Data
    public static class ImmunizationItem {
        private Long id;
        private String fhirId;
        private String externalId;  // Alias for fhirId for external integrations
        private Long patientId; // now inside item
        private String cvxCode;
        private String dateTimeAdministered;
        private String amountAdministered;
        private String expirationDate;
        private String manufacturer;
        private String lotNumber;
        private String administratorName;
        private String administratorTitle;
        private String dateVisGiven;
        private String dateVisStatement;
        private String route;
        private String administrationSite;
        private String notes;
        private String informationSource;
        private String completionStatus;
        private String substanceRefusalReason;
        private String reasonCode;
        private String orderingProvider;
        private Audit audit;
    }

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
