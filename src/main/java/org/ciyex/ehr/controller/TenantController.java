package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.service.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;
import java.util.stream.Collectors;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(
    origins = { "http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001" },
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS },
    allowCredentials = "true"
)
public class TenantController {

    private final KeycloakAdminService keycloakAdminService;

    @GetMapping("/accessible")
    public ResponseEntity<?> getAccessibleTenants() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("No authentication found in security context");
                return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .data(Map.of(
                        "hasFullAccess", false,
                        "tenants", List.of(),
                        "requiresSelection", false
                    ))
                    .build());
            }

            Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

            // ciyex_super_admin: return ALL organizations from Keycloak
            boolean isCiyexSuperAdmin = authorities.contains("ROLE_CIYEX_SUPER_ADMIN");

            List<String> tenants;
            if (isCiyexSuperAdmin) {
                List<Map<String, Object>> allOrgs = keycloakAdminService.getAllOrganizations();
                tenants = allOrgs.stream()
                    .map(org -> (String) org.getOrDefault("alias", org.get("name")))
                    .filter(Objects::nonNull)
                    .sorted()
                    .collect(Collectors.toList());
                log.info("ciyex_super_admin '{}' — returning all {} organizations",
                        authentication.getName(), tenants.size());
            } else {
                // Regular users: extract from JWT groups (legacy /Tenants/ path)
                List<String> groups = authorities.stream()
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.substring(5))
                    .collect(Collectors.toList());

                tenants = groups.stream()
                    .filter(group -> group.contains("/Tenants/"))
                    .map(group -> {
                        String cleanGroup = group.replace("ROLE_", "");
                        String[] parts = cleanGroup.split("/");
                        if (parts.length >= 3) {
                            return parts[2];
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            }

            log.info("User {} accessible tenants: {}", authentication.getName(), tenants);

            boolean requiresSelection = tenants.size() > 1;

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("hasFullAccess", isCiyexSuperAdmin);
            response.put("tenants", tenants);
            response.put("requiresSelection", requiresSelection);
            response.put("isCiyexSuperAdmin", isCiyexSuperAdmin);

            return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Accessible tenants retrieved successfully")
                .data(response)
                .build());

        } catch (Exception e) {
            log.error("Error getting accessible tenants", e);
            return ResponseEntity.ok(ApiResponse.builder()
                .success(false)
                .message("Failed to retrieve accessible tenants: " + e.getMessage())
                .data(Map.of(
                    "hasFullAccess", false,
                    "tenants", List.of(),
                    "requiresSelection", false,
                    "isCiyexSuperAdmin", false
                ))
                .build());
        }
    }
}
