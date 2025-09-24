package com.qiaben.ciyex.dto;

import lombok.Data;
import java.util.List;

@Data
public class ImmunizationDto {
    private Long patientId;
    private Long orgId;
    private Audit audit;
    private List<ImmunizationItem> immunizations;

    @Data
    public static class ImmunizationItem {
        private Long id;
        private String externalId;
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
    }

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
