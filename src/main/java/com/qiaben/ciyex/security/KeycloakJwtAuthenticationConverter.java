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
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    /**
     * ✅ Extracts roles and groups from JWT and maps to Spring authorities.
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<String> roles = new HashSet<>();

        // ✅ 1. Realm roles (e.g., PATIENT, ADMIN, PROVIDER)
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection) {
            roles.addAll((Collection<String>) realmAccess.get("roles"));
        }

        // ✅ 2. Client roles (e.g., from Keycloak client “ciyex-app”)
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null && resourceAccess.get("ciyex-app") instanceof Map) {
            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("ciyex-app");
            if (clientAccess.get("roles") instanceof Collection) {
                roles.addAll((Collection<String>) clientAccess.get("roles"));
            }
        }

        // ✅ 3. Custom single role field (if local tokens use { "role": "PATIENT" })
        if (jwt.hasClaim("role")) {
            roles.add(jwt.getClaimAsString("role"));
        }

        // ✅ 4. Group mappings (optional: turn groups into authorities too)
        List<String> groups = jwt.getClaimAsStringList("groups");
        if (groups != null) {
            roles.addAll(groups);
        }

        // ✅ Convert all roles/groups to Spring authorities format
        Set<GrantedAuthority> authorities = roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> role.toUpperCase().startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

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
}
