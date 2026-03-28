package org.ciyex.ehr.marketplace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.marketplace.dto.*;
import org.ciyex.ehr.marketplace.entity.AppInstallation;
import org.ciyex.ehr.marketplace.repository.AppInstallationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * CDS Hooks service — discovers and invokes CDS services from installed apps.
 *
 * Per HL7 CDS Hooks specification:
 * - Discovery: GET {baseUrl}/cds-services
 * - Invocation: POST {baseUrl}/cds-services/{serviceId}
 *
 * @see <a href="https://cds-hooks.hl7.org/">CDS Hooks Specification</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CdsHooksService {

    private final AppInstallationRepository installationRepository;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** Cache of discovered services per app slug — cleared on app install/uninstall */
    private final Map<String, List<CdsServiceDescriptor>> discoveryCache = new ConcurrentHashMap<>();

    private static final Duration INVOCATION_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Discover all CDS services available for an org.
     * Aggregates services from all installed apps that have CDS Hooks configured.
     */
    @Transactional(readOnly = true)
    public List<CdsServiceDescriptor> discoverServices(String orgId) {
        List<AppInstallation> cdsApps = installationRepository.findCdsHooksApps(orgId);
        List<CdsServiceDescriptor> allServices = new ArrayList<>();

        for (AppInstallation app : cdsApps) {
            try {
                List<CdsServiceDescriptor> services = discoverAppServices(app);
                allServices.addAll(services);
            } catch (Exception e) {
                log.warn("Failed to discover CDS services from app {}: {}", app.getAppSlug(), e.getMessage());
            }
        }

        return allServices;
    }

    /**
     * Discover CDS services for a specific hook type.
     */
    @Transactional(readOnly = true)
    public List<CdsServiceDescriptor> discoverServicesForHook(String orgId, String hookType) {
        return discoverServices(orgId).stream()
                .filter(s -> hookType.equals(s.getHook()))
                .collect(Collectors.toList());
    }

    /**
     * Invoke all CDS services for a given hook type.
     * Calls each service in parallel with a timeout, aggregates cards.
     */
    @Transactional(readOnly = true)
    public CdsHookResponse invokeHook(String orgId, CdsHookRequest request) {
        String hookType = request.getHook();
        String hookJson = "[\"" + hookType + "\"]";
        List<AppInstallation> matchingApps = installationRepository.findByOrgIdAndSupportedHook(orgId, hookJson);

        if (matchingApps.isEmpty()) {
            return CdsHookResponse.builder()
                    .cards(List.of())
                    .servicesInvoked(0)
                    .servicesFailed(0)
                    .build();
        }

        // Collect all services for this hook from matching apps
        List<ServiceTarget> targets = new ArrayList<>();
        for (AppInstallation app : matchingApps) {
            try {
                List<CdsServiceDescriptor> services = discoverAppServices(app);
                for (CdsServiceDescriptor service : services) {
                    if (hookType.equals(service.getHook())) {
                        targets.add(new ServiceTarget(app, service));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to discover services for app {} during hook invocation: {}",
                        app.getAppSlug(), e.getMessage());
            }
        }

        if (targets.isEmpty()) {
            return CdsHookResponse.builder()
                    .cards(List.of())
                    .servicesInvoked(0)
                    .servicesFailed(0)
                    .build();
        }

        // Invoke all services in parallel
        int[] failed = {0};
        List<CompletableFuture<List<CdsCard>>> futures = targets.stream()
                .map(target -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return invokeService(target.app, target.service, request);
                    } catch (Exception e) {
                        log.warn("CDS service invocation failed for {}/{}: {}",
                                target.app.getAppSlug(), target.service.getId(), e.getMessage());
                        failed[0]++;
                        return List.<CdsCard>of();
                    }
                }))
                .toList();

        // Wait for all with timeout
        List<CdsCard> allCards = futures.stream()
                .map(f -> {
                    try {
                        return f.get(INVOCATION_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        failed[0]++;
                        return List.<CdsCard>of();
                    }
                })
                .flatMap(List::stream)
                .sorted(this::cardSeverityComparator)
                .collect(Collectors.toList());

        return CdsHookResponse.builder()
                .cards(allCards)
                .servicesInvoked(targets.size())
                .servicesFailed(failed[0])
                .build();
    }

    /**
     * Clear the discovery cache for an app (called on install/uninstall).
     */
    public void invalidateCache(String appSlug) {
        discoveryCache.remove(appSlug);
    }

    // ── Private helpers ─────────────────────────────────────────────────

    private List<CdsServiceDescriptor> discoverAppServices(AppInstallation app) {
        String slug = app.getAppSlug();

        // Check cache first
        if (discoveryCache.containsKey(slug)) {
            return discoveryCache.get(slug);
        }

        String discoveryUrl = app.getCdsHooksDiscoveryUrl();
        if (discoveryUrl == null || discoveryUrl.isBlank()) {
            return List.of();
        }

        // Ensure URL ends with /cds-services
        String url = discoveryUrl.endsWith("/cds-services")
                ? discoveryUrl
                : discoveryUrl.replaceAll("/+$", "") + "/cds-services";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("CDS discovery returned {} for app {}", response.statusCode(), slug);
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode servicesNode = root.get("services");
            if (servicesNode == null || !servicesNode.isArray()) {
                return List.of();
            }

            List<CdsServiceDescriptor> services = new ArrayList<>();
            for (JsonNode node : servicesNode) {
                CdsServiceDescriptor service = objectMapper.treeToValue(node, CdsServiceDescriptor.class);
                // Annotate with app info
                service.setAppSlug(app.getAppSlug());
                service.setAppName(app.getAppName());
                service.setAppIconUrl(app.getAppIconUrl());
                services.add(service);
            }

            discoveryCache.put(slug, services);
            log.debug("Discovered {} CDS services from app {}", services.size(), slug);
            return services;

        } catch (Exception e) {
            log.warn("CDS discovery failed for app {}: {}", slug, e.getMessage());
            return List.of();
        }
    }

    private List<CdsCard> invokeService(AppInstallation app, CdsServiceDescriptor service, CdsHookRequest hookRequest) {
        String baseUrl = app.getCdsHooksDiscoveryUrl().replaceAll("/+$", "");
        if (!baseUrl.endsWith("/cds-services")) {
            baseUrl = baseUrl + "/cds-services";
        }
        String serviceUrl = baseUrl + "/" + service.getId();

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("hook", hookRequest.getHook());
            payload.put("hookInstance", hookRequest.getHookInstance());
            if (hookRequest.getFhirServer() != null) {
                payload.put("fhirServer", hookRequest.getFhirServer());
            }
            if (hookRequest.getFhirAuthorization() != null) {
                Map<String, String> authz = Map.of(
                        "access_token", hookRequest.getFhirAuthorization(),
                        "token_type", "Bearer",
                        "scope", "patient/*.read"
                );
                payload.put("fhirAuthorization", authz);
            }
            if (hookRequest.getContext() != null) {
                payload.put("context", hookRequest.getContext());
            }
            if (hookRequest.getPrefetch() != null) {
                payload.put("prefetch", hookRequest.getPrefetch());
            }

            String body = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serviceUrl))
                    .timeout(INVOCATION_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("CDS service {}/{} returned {}", app.getAppSlug(), service.getId(), response.statusCode());
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode cardsNode = root.get("cards");
            if (cardsNode == null || !cardsNode.isArray()) {
                return List.of();
            }

            List<CdsCard> cards = new ArrayList<>();
            for (JsonNode cardNode : cardsNode) {
                CdsCard card = objectMapper.treeToValue(cardNode, CdsCard.class);
                // Ensure source is set with app info
                if (card.getSource() == null) {
                    card.setSource(CdsCard.Source.builder()
                            .label(app.getAppName())
                            .icon(app.getAppIconUrl())
                            .build());
                }
                cards.add(card);
            }

            log.debug("Got {} cards from CDS service {}/{}", cards.size(), app.getAppSlug(), service.getId());
            return cards;

        } catch (Exception e) {
            log.warn("CDS invocation failed for {}/{}: {}", app.getAppSlug(), service.getId(), e.getMessage());
            return List.of();
        }
    }

    /** Sort cards: critical first, then warning, then info */
    private int cardSeverityComparator(CdsCard a, CdsCard b) {
        return severityRank(a.getIndicator()) - severityRank(b.getIndicator());
    }

    private int severityRank(String indicator) {
        if (indicator == null) return 3;
        return switch (indicator) {
            case "critical" -> 0;
            case "warning" -> 1;
            case "info" -> 2;
            default -> 3;
        };
    }

    private record ServiceTarget(AppInstallation app, CdsServiceDescriptor service) {}
}
