package com.qiaben.ciyex.security;

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
 * ✅ Converts Keycloak JWT into Spring Security Authentication
 * Supports realm roles, client roles, and groups.
 */
@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        System.out.println("🔍 JWT Converter: Processing JWT token");
        System.out.println("🔍 JWT Subject: " + jwt.getSubject());
        System.out.println("🔍 JWT Claims: " + jwt.getClaims());

        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        System.out.println("🔍 Extracted Authorities: " + authorities);

        return new JwtAuthenticationToken(jwt, authorities);
    }

    /**
     * ✅ Extracts roles and groups from JWT and maps to Spring authorities.
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<String> roles = new HashSet<>();

        System.out.println("🔍 Extracting authorities from JWT...");

        // ✅ 1. Realm roles (e.g., PATIENT, ADMIN, PROVIDER)
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection) {
            roles.addAll((Collection<String>) realmAccess.get("roles"));
            System.out.println("🔍 Realm roles found: " + realmAccess.get("roles"));
        } else {
            System.out.println("🔍 No realm_access roles found");
        }

        // ✅ 2. Client roles (e.g., from Keycloak client "ciyex-app")
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null && resourceAccess.get("ciyex-app") instanceof Map) {
            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("ciyex-app");
            if (clientAccess.get("roles") instanceof Collection) {
                roles.addAll((Collection<String>) clientAccess.get("roles"));
                System.out.println("🔍 Client roles found: " + clientAccess.get("roles"));
            }
        } else {
            System.out.println("🔍 No resource_access or ciyex-app client roles found");
        }

        // ✅ 3. Custom single role field (if local tokens use { "role": "PATIENT" })
        if (jwt.hasClaim("role")) {
            String role = jwt.getClaimAsString("role");
            roles.add(role);
            System.out.println("🔍 Custom role claim found: " + role);
        } else {
            System.out.println("🔍 No custom role claim found");
        }

        // ✅ 4. Group mappings (optional: turn groups into authorities too)
        List<String> groups = jwt.getClaimAsStringList("groups");
        if (groups != null) {
            roles.addAll(groups);
            System.out.println("🔍 Groups found: " + groups);
        } else {
            System.out.println("🔍 No groups found");
        }

        System.out.println("🔍 Raw roles before processing: " + roles);

        // ✅ Convert all roles/groups to Spring authorities format
        Set<GrantedAuthority> authorities = roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> role.toUpperCase().startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        System.out.println("🔍 Final authorities: " + authorities);

        return authorities;
    }

    // ✅ Utility helper methods
    public static String extractEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return (email != null) ? email : jwt.getClaimAsString("preferred_username");
    }

    public static String extractFullName(Jwt jwt) {
        String given = jwt.getClaimAsString("given_name");
        String family = jwt.getClaimAsString("family_name");
        return (given != null ? given : "") + (family != null ? " " + family : "");
    }

    public static List<String> extractGroups(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("groups");
        return groups != null ? groups : Collections.emptyList();
    }

    /**
     * ✅ Extract organization/practice ID from JWT token.
     * Keycloak organization claim format: ["org-alias", {"org-alias": {"id": "uuid"}}]
     * or [{"org-alias": {"id": "uuid"}}, "org-alias"]
     */
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
