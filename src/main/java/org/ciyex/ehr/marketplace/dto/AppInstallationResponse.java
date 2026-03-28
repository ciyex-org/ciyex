package org.ciyex.ehr.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppInstallationResponse {
    private UUID id;
    private String orgId;
    private UUID appId;
    private String appSlug;
    private String appName;
    private String appIconUrl;
    private String appCategory;
    private UUID subscriptionId;
    private String status;
    private Map<String, Object> config;
    private List<String> extensionPoints;
    private String smartLaunchUrl;
    private List<String> fhirScopes;
    private String keycloakClientId;
    private String installedBy;
    private LocalDateTime installedAt;
    private LocalDateTime updatedAt;
}
