package org.ciyex.ehr.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.marketplace.dto.InstallAppRequest;
import org.ciyex.ehr.marketplace.dto.MarketplaceWebhookPayload;
import org.ciyex.ehr.marketplace.service.AppInstallationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/api/internal/marketplace-webhook")
@RequiredArgsConstructor
@Slf4j
public class InternalWebhookController {

    private final AppInstallationService installationService;
    private final ObjectMapper objectMapper;

    @Value("${marketplace.webhook-secret:}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Marketplace-Signature", required = false) String signature) {

        // Validate HMAC signature
        if (!verifySignature(rawBody, signature)) {
            log.warn("Invalid webhook signature received");
            return ResponseEntity.status(401).body(Map.of("error", "Invalid signature"));
        }

        try {
            MarketplaceWebhookPayload payload = objectMapper.readValue(rawBody, MarketplaceWebhookPayload.class);
            String event = payload.getEvent();
            String orgAlias = payload.getSubscription().getOrgAlias();

            log.info("Received marketplace webhook: event={}, org={}, app={}",
                    event, orgAlias, payload.getApp().getSlug());

            switch (event) {
                case "subscription.created" -> handleSubscriptionCreated(payload, orgAlias);
                case "subscription.cancelled" -> handleSubscriptionCancelled(payload, orgAlias);
                case "subscription.paused" -> handleSubscriptionPaused(payload, orgAlias);
                default -> log.warn("Unknown webhook event: {}", event);
            }

            return ResponseEntity.ok(Map.of("status", "processed"));
        } catch (Exception e) {
            log.error("Failed to process marketplace webhook", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to process webhook"));
        }
    }

    private void handleSubscriptionCreated(MarketplaceWebhookPayload payload, String orgAlias) {
        var app = payload.getApp();
        var sub = payload.getSubscription();

        InstallAppRequest request = InstallAppRequest.builder()
                .appId(app.getId())
                .appSlug(app.getSlug())
                .appName(app.getName())
                .appIconUrl(app.getIconUrl())
                .appCategory(app.getCategory())
                .subscriptionId(sub.getId())
                .extensionPoints(app.getExtensionPoints())
                .cdsHooksDiscoveryUrl(app.getCdsHooksDiscoveryUrl())
                .supportedHooks(app.getSupportedHooks())
                .smartLaunchUrl(app.getSmartLaunchUrl())
                .smartRedirectUris(app.getSmartRedirectUris())
                .fhirScopes(app.getFhirScopes())
                .serviceUrl(app.getServiceUrl())
                .build();

        installationService.installApp(orgAlias, "marketplace-webhook", request);
        log.info("Auto-installed app {} for org {} via webhook", app.getSlug(), orgAlias);
    }

    private void handleSubscriptionCancelled(MarketplaceWebhookPayload payload, String orgAlias) {
        String appSlug = payload.getApp().getSlug();
        try {
            installationService.uninstallApp(orgAlias, appSlug);
            log.info("Auto-uninstalled app {} for org {} via webhook", appSlug, orgAlias);
        } catch (IllegalArgumentException e) {
            log.warn("App {} not found for org {} during cancellation webhook", appSlug, orgAlias);
        }
    }

    private void handleSubscriptionPaused(MarketplaceWebhookPayload payload, String orgAlias) {
        String appSlug = payload.getApp().getSlug();
        installationService.suspendApp(orgAlias, appSlug);
        log.info("Suspended app {} for org {} via webhook", appSlug, orgAlias);
    }

    private boolean verifySignature(String body, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("Webhook secret is not configured — rejecting request");
            return false;
        }

        if (signature == null || signature.isBlank()) {
            log.warn("Missing webhook signature header");
            return false;
        }

        try {
            String expected = "sha256=" + computeHmacSha256(body, webhookSecret);
            return expected.equals(signature);
        } catch (Exception e) {
            log.error("HMAC verification failed", e);
            return false;
        }
    }

    private String computeHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
