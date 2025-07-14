package com.qiaben.ciyex.dto.fhir;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FhirEncounterDTO {
    private String id;
    private MetaDTO meta;
    private String resourceType;
    private String status;
    private String type;
    private String subject;

    @Data
    @NoArgsConstructor
    public static class MetaDTO {
        private String versionId;
        private String lastUpdated;
    }
}
