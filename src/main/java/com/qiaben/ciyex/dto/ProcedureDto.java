

package com.qiaben.ciyex.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProcedureDto {
    private Long id;
    private String externalId;
    private String fhirId;
    private Long patientId;
    private Long encounterId;

    private String cpt4;                 // CPT code
    private String description;
    private Integer units;
    private String rate;

    private String relatedIcds;
    private String hospitalBillingStart;
    private String hospitalBillingEnd;

    private String modifier1;


    private Integer priceLevelId;
    private String priceLevelTitle;
    private String note;
    private String providername;

    // Support for multiple code types in single procedure
    private List<CodeItem> codeItems;

    private Audit audit;

    @Data
    public static class CodeItem {
        private String cpt4;
        private String description;
        private Integer units;
        private String rate;
        private String relatedIcds;
        private String modifier1;

        private String hospitalBillingStart;
        private String hospitalBillingEnd;
        private String note;
        private Integer priceLevelId;
        private String priceLevelTitle;
        private String providername;
    }
    @Data
    public static class Audit {
        private String createdDate;       // yyyy-MM-dd
        private String lastModifiedDate;  // yyyy-MM-dd
    }
}
