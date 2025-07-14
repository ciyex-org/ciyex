package com.qiaben.ciyex.dto.fhir;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GroupResponseDTO {
    private String resourceType;
    private String type;
    private int total;
    private List<Link> link;
    private Meta meta;

    @Data
    @NoArgsConstructor
    public static class Link {
        private String relation;
        private String url;
    }

    @Data
    @NoArgsConstructor
    public static class Meta {
        private String lastUpdated;
    }
}
