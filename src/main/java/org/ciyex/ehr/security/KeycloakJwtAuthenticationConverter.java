package org.ciyex.ehr.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts a Keycloak-issued JWT into a Spring Security {@link JwtAuthenticationToken}
 * carrying both role-based and SMART on FHIR scope-based {@link GrantedAuthority} objects.
 *
 * <h3>Granted authority naming</h3>
 * <ul>
 *   <li>{@code ROLE_ADMIN}, {@code ROLE_PROVIDER}, … — Keycloak realm / client roles</li>
 *   <li>{@code SCOPE_user/Patient.read}, … — SMART scopes, either present in the JWT
 *       {@code scope} claim, or automatically expanded from roles via
 *       {@link RolePermissionRegistry}</li>
 * </ul>
 *
 * <h3>Authority expansion</h3>
 * Every recognised {@link ClinicalRole} in the JWT is automatically expanded to its
 * full set of SMART scope authorities via {@link RolePermissionRegistry}. This means
 * controllers can rely on {@code @PreAuthorize("hasAuthority('SCOPE_user/Patient.read')")}
 * without any additional configuration in Keycloak.
 *
 * <h3>Keycloak configuration required</h3>
 * <ol>
 *   <li>Define realm roles matching {@link ClinicalRole} enum names
 *       (ADMIN, PROVIDER, NURSE, MA, FRONT_DESK, BILLING, PATIENT, super_admin)</li>
 *   <li>Assign roles to users or groups</li>
 *   <li>Client {@code ciyex-app} receives roles via {@code realm_access} in token (default)</li>
 *   <li>OPTIONAL: define SMART Client Scopes in Keycloak to also include SMART scopes
 *       directly in the JWT {@code scope} claim — both are honoured</li>
 * </ol>
 */
@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    /** Keycloak client ID for the EHR web application. */
    private static final String CLIENT_ID = "ciyex-app";

    /** Keycloak internal roles that should never be treated as EHR application roles. */
    private static final Set<String> EXCLUDED_ROLES = Set.of(
            "OFFLINE_ACCESS", "UMA_AUTHORIZATION", "DEFAULT-ROLES-CIYEX"
    );

    private final SmartScopeResolver smartScopeResolver;

    public KeycloakJwtAuthenticationConverter(SmartScopeResolver smartScopeResolver) {
        this.smartScopeResolver = smartScopeResolver;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // ── 1. Extract Keycloak roles ──────────────────────────────────────────
        Set<String> realmRoles  = new LinkedHashSet<>(extractRealmRoles(jwt));
        Set<String> clientRoles = extractClientRoles(jwt);

        // Also pick up single "role" claim (used by portal JWTs)
        String singleRole = jwt.getClaimAsString("role");
        if (singleRole != null && !singleRole.isBlank()) {
            realmRoles.add(singleRole.toUpperCase());
        }

        Set<String> allRoleNames = new LinkedHashSet<>();
        allRoleNames.addAll(realmRoles);
        allRoleNames.addAll(clientRoles);

        // Grant ROLE_ authority for every role (including custom roles from role_permission_config)
        for (String roleName : allRoleNames) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()));
        }

        // super_admin is a realm role but may use lowercase in Keycloak
        if (allRoleNames.stream().anyMatch(r -> r.equalsIgnoreCase("super_admin"))) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
            allRoleNames.add("SUPER_ADMIN");
        }

        // ciyex_super_admin: platform-level role for cross-org access
        // Also grants ADMIN-level SMART scopes so they have full access in any org
        if (allRoleNames.stream().anyMatch(r -> r.equalsIgnoreCase("ciyex_super_admin"))) {
            authorities.add(new SimpleGrantedAuthority("ROLE_CIYEX_SUPER_ADMIN"));
            // Ensure they have ADMIN-level permissions in whichever org they switch to
            if (!allRoleNames.contains("ADMIN")) {
                allRoleNames.add("ADMIN");
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
        }

        // ── 2. Expand roles → SMART scope authorities ──────────────────────────
        // Resolve scopes from DB (role_permission_config.smart_scopes) with
        // fallback to static RolePermissionRegistry if no DB row exists.
        String orgAlias = extractOrganizationAlias(jwt);
        smartScopeResolver.resolveScopes(orgAlias, allRoleNames).stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        // ── 3. Honour explicit SMART scopes from JWT scope claim ───────────────
        // If Keycloak issues SMART scopes directly, add them too.
        String scopeClaim = jwt.getClaimAsString("scope");
        if (scopeClaim != null && !scopeClaim.isBlank()) {
            Arrays.stream(scopeClaim.split("\\s+"))
                    .filter(s -> !s.isBlank())
                    .filter(s -> s.contains("/") || s.startsWith("patient/")
                            || s.startsWith("user/") || s.startsWith("system/"))
                    .map(s -> new SimpleGrantedAuthority("SCOPE_" + s))
                    .forEach(authorities::add);
        }

        return new JwtAuthenticationToken(jwt, authorities);
    }

    // ── JWT claim extraction ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Set<String> extractRealmRoles(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();
        try {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof List<?> list) {
                for (Object r : list) {
                    if (r instanceof String s && !s.isBlank()) {
                        String upper = s.toUpperCase();
                        if (!EXCLUDED_ROLES.contains(upper)) {
                            roles.add(upper);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return roles;
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractClientRoles(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();
        try {
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null && resourceAccess.get(CLIENT_ID) instanceof Map<?, ?> clientMap) {
                if (clientMap.get("roles") instanceof List<?> list) {
                    for (Object r : list) {
                        if (r instanceof String s && !s.isBlank()) {
                            String upper = s.toUpperCase();
                            if (!EXCLUDED_ROLES.contains(upper)) {
                                roles.add(upper);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return roles;
    }

    // ── Static helpers used by RequestContextInterceptor ─────────────────────

    public static String extractEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return email != null ? email : jwt.getClaimAsString("preferred_username");
    }

    public static String extractFullName(Jwt jwt) {
        String given  = jwt.getClaimAsString("given_name");
        String family = jwt.getClaimAsString("family_name");
        return (given != null ? given : "") + (family != null ? " " + family : "");
    }

    public static List<String> extractGroups(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("groups");
        return groups != null ? groups : Collections.emptyList();
    }

    /**
     * Extracts the primary organisation alias from the {@code organization} JWT claim.
     * Supports both flat string list and nested map forms used by Keycloak Organizations.
     */
    public static String extractOrganizationAlias(Jwt jwt) {
        Object orgClaim = jwt.getClaim("organization");
        if (orgClaim instanceof List<?> orgArray && !orgArray.isEmpty()) {
            // Flat list: ["sunrise-family-medicine"]
            for (Object elem : orgArray) {
                if (elem instanceof String alias && !alias.isBlank()) {
                    return alias;
                }
            }
            // Map form: [{"sunrise-family-medicine": {...}}]
            for (Object elem : orgArray) {
                if (elem instanceof Map<?, ?> orgDetails) {
                    for (Object key : orgDetails.keySet()) {
                        if (key instanceof String alias && !alias.isBlank()) {
                            return alias;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** Extracts the Keycloak organisation ID from the JWT {@code organization} claim. */
    public static String extractOrganizationId(Jwt jwt) {
        Object orgClaim = jwt.getClaim("organization");
        if (orgClaim instanceof List<?> orgArray && !orgArray.isEmpty()) {
            for (Object elem : orgArray) {
                if (elem instanceof Map<?, ?> orgDetails) {
                    for (Object details : orgDetails.values()) {
                        if (details instanceof Map<?, ?> detailMap) {
                            Object orgId = detailMap.get("id");
                            if (orgId instanceof String id && !id.isBlank()) {
                                return id;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
