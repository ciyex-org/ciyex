package org.ciyex.ehr.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallAppRequest {
    private UUID appId;
    private String appSlug;
    private String appName;
    private String appIconUrl;
    private String appCategory;
    private UUID subscriptionId;
    private List<String> extensionPoints;
    private String cdsHooksDiscoveryUrl;
    private List<String> supportedHooks;
    private String smartLaunchUrl;
    private List<String> smartRedirectUris;
    private List<String> fhirScopes;
    private String serviceUrl;
}
