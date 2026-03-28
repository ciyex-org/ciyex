package org.ciyex.ehr.marketplace.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.marketplace.dto.AppInstallationResponse;
import org.ciyex.ehr.marketplace.dto.InstallAppRequest;
import org.ciyex.ehr.marketplace.dto.LaunchRequest;
import org.ciyex.ehr.marketplace.entity.AppInstallation;
import org.ciyex.ehr.marketplace.entity.AppLaunchLog;
import org.ciyex.ehr.marketplace.repository.AppInstallationRepository;
import org.ciyex.ehr.marketplace.repository.AppLaunchLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppInstallationService {

    private final AppInstallationRepository installationRepository;
    private final AppLaunchLogRepository launchLogRepository;
    private final AppUsageService usageService;
    private final KeycloakSmartClientService keycloakSmartClientService;

    @Transactional(readOnly = true)
    public List<AppInstallationResponse> getInstalledApps(String orgId) {
        return installationRepository.findByOrgIdAndStatusNot(orgId, "uninstalled")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppInstallationResponse getInstallation(String orgId, String appSlug) {
        return installationRepository.findByOrgIdAndAppSlug(orgId, appSlug)
                .filter(i -> !"uninstalled".equals(i.getStatus()))
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean isInstalled(String orgId, String appSlug) {
        return installationRepository.existsByOrgIdAndAppSlugAndStatus(orgId, appSlug, "active");
    }

    @Transactional
    public AppInstallationResponse installApp(String orgId, String installedBy, InstallAppRequest request) {
        // Check if already installed (could be re-installing after uninstall)
        var existing = installationRepository.findByOrgIdAndAppSlug(orgId, request.getAppSlug());
        if (existing.isPresent()) {
            AppInstallation installation = existing.get();
            if ("active".equals(installation.getStatus())) {
                log.warn("App {} already installed for org {}", request.getAppSlug(), orgId);
                return toResponse(installation);
            }
            // Re-activate previously uninstalled app
            installation.setStatus("active");
            installation.setAppId(request.getAppId());
            installation.setAppName(request.getAppName());
            installation.setAppIconUrl(request.getAppIconUrl());
            installation.setAppCategory(request.getAppCategory());
            installation.setSubscriptionId(request.getSubscriptionId());
            installation.setExtensionPoints(request.getExtensionPoints() != null ? request.getExtensionPoints() : List.of());
            installation.setCdsHooksDiscoveryUrl(request.getCdsHooksDiscoveryUrl());
            installation.setSupportedHooks(request.getSupportedHooks() != null ? request.getSupportedHooks() : List.of());
            installation.setSmartLaunchUrl(request.getSmartLaunchUrl());
            installation.setSmartRedirectUris(request.getSmartRedirectUris() != null ? request.getSmartRedirectUris() : List.of());
            installation.setFhirScopes(request.getFhirScopes() != null ? request.getFhirScopes() : List.of());
            installation.setInstalledBy(installedBy);
            installation.setUninstalledAt(null);

            // Register Keycloak client for SMART apps
            registerKeycloakClientIfSmart(installation, request);

            installation = installationRepository.save(installation);
            log.info("Re-activated app {} for org {}", request.getAppSlug(), orgId);
            return toResponse(installation);
        }

        AppInstallation installation = AppInstallation.builder()
                .orgId(orgId)
                .appId(request.getAppId())
                .appSlug(request.getAppSlug())
                .appName(request.getAppName())
                .appIconUrl(request.getAppIconUrl())
                .appCategory(request.getAppCategory())
                .subscriptionId(request.getSubscriptionId())
                .extensionPoints(request.getExtensionPoints() != null ? request.getExtensionPoints() : List.of())
                .cdsHooksDiscoveryUrl(request.getCdsHooksDiscoveryUrl())
                .supportedHooks(request.getSupportedHooks() != null ? request.getSupportedHooks() : List.of())
                .smartLaunchUrl(request.getSmartLaunchUrl())
                .smartRedirectUris(request.getSmartRedirectUris() != null ? request.getSmartRedirectUris() : List.of())
                .fhirScopes(request.getFhirScopes() != null ? request.getFhirScopes() : List.of())
                .status("active")
                .config(buildInitialConfig(request))
                .installedBy(installedBy)
                .build();

        // Register Keycloak client for SMART apps
        registerKeycloakClientIfSmart(installation, request);

        installation = installationRepository.save(installation);
        log.info("Installed app {} for org {} by {}", request.getAppSlug(), orgId, installedBy);
        return toResponse(installation);
    }

    @Transactional
    public void uninstallApp(String orgId, String appSlug) {
        var installation = installationRepository.findByOrgIdAndAppSlug(orgId, appSlug)
                .orElseThrow(() -> new IllegalArgumentException("App not installed: " + appSlug));

        installation.setStatus("uninstalled");
        installation.setUninstalledAt(LocalDateTime.now());
        installationRepository.save(installation);

        // Remove Keycloak client for SMART apps
        if (installation.getSmartLaunchUrl() != null && !installation.getSmartLaunchUrl().isBlank()) {
            try {
                keycloakSmartClientService.removeSmartClient(appSlug);
            } catch (Exception e) {
                log.debug("Failed to remove Keycloak client for {}: {}", appSlug, e.getMessage());
            }
        }

        log.info("Uninstalled app {} for org {}", appSlug, orgId);
    }

    @Transactional
    public AppInstallationResponse updateConfig(String orgId, String appSlug, Map<String, Object> config) {
        var installation = installationRepository.findByOrgIdAndAppSlug(orgId, appSlug)
                .orElseThrow(() -> new IllegalArgumentException("App not installed: " + appSlug));

        installation.setConfig(config);
        installation = installationRepository.save(installation);
        log.info("Updated config for app {} org {}", appSlug, orgId);
        return toResponse(installation);
    }

    @Transactional
    public void suspendApp(String orgId, String appSlug) {
        installationRepository.findByOrgIdAndAppSlug(orgId, appSlug).ifPresent(installation -> {
            installation.setStatus("suspended");
            installationRepository.save(installation);
            log.info("Suspended app {} for org {}", appSlug, orgId);
        });
    }

    @Transactional
    public void logLaunch(String orgId, String appSlug, String launchedBy, LaunchRequest request) {
        var installation = installationRepository.findByOrgIdAndAppSlug(orgId, appSlug).orElse(null);

        AppLaunchLog launchLog = AppLaunchLog.builder()
                .orgId(orgId)
                .appInstallationId(installation != null ? installation.getId() : null)
                .appSlug(appSlug)
                .launchedBy(launchedBy)
                .patientId(request.getPatientId())
                .encounterId(request.getEncounterId())
                .launchType(request.getLaunchType() != null ? request.getLaunchType() : "native")
                .build();

        launchLogRepository.save(launchLog);

        // Record usage event for metering
        try {
            usageService.recordEvent(orgId, appSlug, "app_launch",
                    request.getLaunchType(), launchedBy, request.getPatientId(), request.getEncounterId());
        } catch (Exception e) {
            log.debug("Failed to record usage event for app launch: {}", e.getMessage());
        }

        log.debug("Logged launch of app {} by {} in org {}", appSlug, launchedBy, orgId);
    }

    /**
     * Get installed apps that register for any extension point in the given context.
     * Context is a prefix like "patient-chart" which matches "patient-chart:tab", "patient-chart:action-bar", etc.
     */
    @Transactional(readOnly = true)
    public List<AppInstallationResponse> getAppsForContext(String orgId, String context) {
        return installationRepository.findByOrgIdAndExtensionPointPrefix(orgId, context + ":%")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get installed apps that register for a specific slot name (exact match).
     */
    @Transactional(readOnly = true)
    public List<AppInstallationResponse> getAppsForSlot(String orgId, String slotName) {
        String slotJson = "[\"" + slotName + "\"]";
        return installationRepository.findByOrgIdAndExtensionPoint(orgId, slotJson)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Register a Keycloak OAuth2 client if the app has a SMART launch URL.
     */
    private void registerKeycloakClientIfSmart(AppInstallation installation, InstallAppRequest request) {
        if (request.getSmartLaunchUrl() == null || request.getSmartLaunchUrl().isBlank()) {
            return;
        }
        try {
            String keycloakClientId = keycloakSmartClientService.registerSmartClient(
                    request.getAppSlug(),
                    request.getAppName(),
                    request.getSmartRedirectUris(),
                    request.getFhirScopes(),
                    true // default to public client (SPA)
            );
            installation.setKeycloakClientId(keycloakClientId);
        } catch (Exception e) {
            log.warn("Failed to register Keycloak client for SMART app {}: {}",
                    request.getAppSlug(), e.getMessage());
            // Non-fatal — app is still installed, Keycloak client can be created later
            installation.setKeycloakClientId(keycloakSmartClientService.toClientId(request.getAppSlug()));
        }
    }

    /**
     * Build initial config for a new installation.
     * Includes service_url from marketplace if the app provides one.
     */
    private Map<String, Object> buildInitialConfig(InstallAppRequest request) {
        Map<String, Object> config = new HashMap<>();
        if (request.getServiceUrl() != null && !request.getServiceUrl().isBlank()) {
            config.put("service_url", request.getServiceUrl());
        }
        return config;
    }

    private AppInstallationResponse toResponse(AppInstallation entity) {
        return AppInstallationResponse.builder()
                .id(entity.getId())
                .orgId(entity.getOrgId())
                .appId(entity.getAppId())
                .appSlug(entity.getAppSlug())
                .appName(entity.getAppName())
                .appIconUrl(entity.getAppIconUrl())
                .appCategory(entity.getAppCategory())
                .subscriptionId(entity.getSubscriptionId())
                .status(entity.getStatus())
                .config(entity.getConfig())
                .extensionPoints(entity.getExtensionPoints())
                .smartLaunchUrl(entity.getSmartLaunchUrl())
                .fhirScopes(entity.getFhirScopes())
                .keycloakClientId(entity.getKeycloakClientId())
                .installedBy(entity.getInstalledBy())
                .installedAt(entity.getInstalledAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
