package org.ciyex.ehr.fhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.service.PracticeContextService;
import org.ciyex.ehr.tabconfig.entity.TabFieldConfig;
import org.ciyex.ehr.tabconfig.service.TabFieldConfigService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enforces per-resource-type SMART on FHIR scopes for the generic FHIR controller.
 *
 * <p>Derives the required scope from {@code tab_field_config.fhir_resources} JSONB.
 * For example, if a tab's primary FHIR resource is {@code Appointment}, reading
 * requires {@code SCOPE_user/Appointment.read} and writing requires
 * {@code SCOPE_user/Appointment.write}.</p>
 *
 * <p>All users including admins are subject to per-resource scope checks.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FhirScopeEnforcer {

    private final TabFieldConfigService tabFieldConfigService;
    private final PracticeContextService practiceContextService;
    private final ObjectMapper objectMapper;

    /**
     * Check that the current user has the appropriate SMART scope for reading
     * the FHIR resource type mapped by the given tabKey.
     *
     * @throws AccessDeniedException if the user lacks the required scope
     */
    public void requireRead(String tabKey) {
        requireScope(tabKey, "read");
    }

    /**
     * Check that the current user has the appropriate SMART scope for writing
     * the FHIR resource type mapped by the given tabKey.
     *
     * @throws AccessDeniedException if the user lacks the required scope
     */
    public void requireWrite(String tabKey) {
        requireScope(tabKey, "write");
    }

    /**
     * Reference data tabs that should be readable by any authenticated user.
     * These are used in dropdowns, lookups, and autocomplete across the app.
     * The Settings UI is protected separately by menu_item.required_permission.
     */
    private static final Set<String> REFERENCE_DATA_TABS = Set.of(
            "providers", "facilities", "services", "healthcareservices",
            "referral-practices", "referral-providers"
    );

    private void requireScope(String tabKey, String action) {
        // Reference data tabs are readable by any authenticated user
        if ("read".equals(action) && REFERENCE_DATA_TABS.contains(tabKey)) {
            log.debug("Tab '{}' is reference data — skipping scope check for read", tabKey);
            return;
        }

        Set<String> authorities = getCurrentAuthorities();

        String resourceType = resolvePrimaryResourceType(tabKey);
        if (resourceType == null) {
            // For write operations, deny by default when resource type cannot be resolved
            // to prevent privilege escalation through misconfigured or missing tab configs
            if ("write".equals(action)) {
                log.warn("Access denied: no FHIR resource type mapped for tab '{}' — denying write by default", tabKey);
                throw new AccessDeniedException(
                        "Cannot determine resource type for tab '" + tabKey + "' — write access denied");
            }
            log.debug("No FHIR resource type mapped for tab '{}' — allowing read", tabKey);
            return;
        }

        String userScope = "SCOPE_user/" + resourceType + "." + action;
        String patientScope = "SCOPE_patient/" + resourceType + "." + action;

        if (!authorities.contains(userScope) && !authorities.contains(patientScope)) {
            log.warn("Access denied: user lacks {} or {} for tab '{}' (resource type: {})",
                    userScope, patientScope, tabKey, resourceType);
            throw new AccessDeniedException(
                    "You do not have permission to " + action + " " + resourceType + " resources");
        }
        log.debug("Scope check passed for tab '{}' (resource type: {})", tabKey, resourceType);
    }

    /**
     * Extracts the primary FHIR resource type from tab_field_config.fhir_resources.
     * Returns null if the tab has no FHIR resource mapping.
     */
    private String resolvePrimaryResourceType(String tabKey) {
        try {
            String orgId;
            try {
                orgId = practiceContextService.getPracticeId();
            } catch (Exception e) {
                orgId = "*";
            }
            TabFieldConfig config = tabFieldConfigService.getEffectiveFieldConfig(tabKey, "*", orgId);
            if (config == null || config.getFhirResources() == null) {
                log.debug("Tab '{}': config={}, fhirResources=null", tabKey, config != null ? "found" : "null");
                return null;
            }

            String fhirResourcesJson = config.getFhirResources();
            log.debug("Tab '{}': fhirResources='{}', orgId='{}'", tabKey, fhirResourcesJson, config.getOrgId());
            JsonNode resources = objectMapper.readTree(fhirResourcesJson);
            if (!resources.isArray() || resources.isEmpty()) {
                return null;
            }

            JsonNode first = resources.get(0);
            if (first.isTextual()) {
                return first.asText(); // Simple format: "Appointment"
            } else if (first.isObject() && first.has("type")) {
                return first.get("type").asText(); // Object format: {"type":"Appointment",...}
            }
        } catch (Exception e) {
            log.debug("Could not resolve resource type for tab '{}': {}", tabKey, e.getMessage());
        }
        return null;
    }

    private Set<String> getCurrentAuthorities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
