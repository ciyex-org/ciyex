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
public class MarketplaceWebhookPayload {
    private String event;
    private AppInfo app;
    private SubscriptionInfo subscription;
    private String timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppInfo {
        private UUID id;
        private String slug;
        private String name;
        private String iconUrl;
        private String category;
        private List<String> extensionPoints;
        private String cdsHooksDiscoveryUrl;
        private List<String> supportedHooks;
        private String smartLaunchUrl;
        private List<String> smartRedirectUris;
        private List<String> fhirScopes;
        private String serviceUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionInfo {
        private UUID id;
        private String status;
        private String orgAlias;
    }
}
