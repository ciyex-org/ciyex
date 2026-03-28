package org.ciyex.ehr.portal.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PortalConfigDto {
    private Long id;
    private Boolean enabled;
    private String portalUrl;
    private String features;   // JSON string
    private String branding;   // JSON string
    private String registration; // JSON string
    private String createdAt;
    private String updatedAt;
}
