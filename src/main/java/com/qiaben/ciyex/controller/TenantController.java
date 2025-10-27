package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tenants")
@Slf4j
@CrossOrigin(
    origins = { "http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001" },
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS },
    allowCredentials = "true"
)
public class TenantController {

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

            // Extract groups from JWT token (set by JwtAuthenticationFilter)
            // Groups are stored as authorities with ROLE_ prefix
            List<String> groups = authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5)) // Remove "ROLE_" prefix
                .collect(Collectors.toList());

            log.info("User {} has groups: {}", authentication.getName(), groups);

            // Extract tenant names from group paths
            // Groups format: "ROLE_/Tenants/CareWell", "ROLE_/Tenants/Qiaben Health"
            List<String> tenants = groups.stream()
                .filter(group -> group.contains("/Tenants/"))
                .map(group -> {
                    // Remove ROLE_ prefix if present
                    String cleanGroup = group.replace("ROLE_", "");
                    // Extract tenant name from path like "/Tenants/CareWell"
                    String[] parts = cleanGroup.split("/");
                    if (parts.length >= 3) {
                        return parts[2]; // Get the tenant name part
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

            log.info("Extracted tenants: {}", tenants);

            boolean requiresSelection = tenants.size() > 1;
            boolean hasFullAccess = groups.stream()
                .anyMatch(g -> g.toUpperCase().contains("ADMIN") || g.toUpperCase().contains("FULL_ACCESS"));

            Map<String, Object> response = Map.of(
                "hasFullAccess", hasFullAccess,
                "tenants", tenants,
                "requiresSelection", requiresSelection
            );

            log.info("Returning tenant response: {}", response);

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
                    "requiresSelection", false
                ))
                .build());
        }
    }
}
