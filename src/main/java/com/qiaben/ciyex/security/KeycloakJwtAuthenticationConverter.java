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
 * Converts Keycloak JWT tokens to Spring Security Authentication
 * Maps Keycloak groups to Spring Security authorities (replacing tenant concept)
 */
@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    /**
     * Extract authorities from JWT token
     * Includes groups (replacing tenant) and realm roles
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Extract groups (these replace the tenant concept)
        List<String> groups = jwt.getClaimAsStringList("groups");
        if (groups != null) {
            authorities.addAll(groups.stream()
                    .map(group -> new SimpleGrantedAuthority("GROUP_" + group.toUpperCase()))
                    .collect(Collectors.toList()));
        }

        // Extract realm roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            authorities.addAll(roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList()));
        }

        // Extract resource/client roles
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            resourceAccess.values().forEach(resource -> {
                if (resource instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resourceMap = (Map<String, Object>) resource;
                    if (resourceMap.containsKey("roles")) {
                        @SuppressWarnings("unchecked")
                        List<String> roles = (List<String>) resourceMap.get("roles");
                        authorities.addAll(roles.stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                                .collect(Collectors.toList()));
                    }
                }
            });
        }

        return authorities;
    }

    /**
     * Extract groups from JWT (for use in application logic)
     */
    public static List<String> extractGroups(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("groups");
        return groups != null ? groups : Collections.emptyList();
    }

    /**
     * Extract user email from JWT
     */
    public static String extractEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = jwt.getClaimAsString("preferred_username");
        }
        return email;
    }

    /**
     * Extract user's full name from JWT
     */
    public static String extractFullName(Jwt jwt) {
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        
        if (givenName != null && familyName != null) {
            return givenName + " " + familyName;
        } else if (givenName != null) {
            return givenName;
        } else if (familyName != null) {
            return familyName;
        }
        
        return jwt.getClaimAsString("preferred_username");
    }
}
