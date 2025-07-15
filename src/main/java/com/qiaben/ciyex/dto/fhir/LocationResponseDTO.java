package com.qiaben.ciyex.dto.fhir;

import lombok.Data;

@Data
public class LocationResponseDTO {
    private String resourceType;
    private Meta meta;
    private Link[] link;
    private Integer total;

    @Data
    public static class Meta {
        private String lastUpdated;
    }

    @Data
    public static class Link {
        private String relation;
        private String url;
    }
}
